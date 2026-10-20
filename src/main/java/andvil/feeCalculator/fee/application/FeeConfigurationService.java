package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import andvil.feeCalculator.fee.model.*;
import andvil.feeCalculator.fee.persistence.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Manages active fee rules. Writes reject missing or malformed values with status 400,
 * duplicate keys or overlapping ranges with status 409, and unknown IDs with status 404
 * through {@link ConfigurationException}. Fees are non-negative euro amounts fitting
 * DECIMAL(10,2); thresholds must fit DECIMAL(8,3).
 */
@Service
public class FeeConfigurationService {
    private static final Sort ID_ORDER = Sort.by("id");
    private final RegionalBaseFeeRepository baseFees;
    private final TemperatureFeeRuleRepository temperatures;
    private final WindFeeRuleRepository winds;
    private final PhenomenonFeeRuleRepository phenomena;
    private final TransactionTemplate transactions;
    private final Object writeLock = new Object();

    /** Creates the service with the four rule repositories and transaction support. */
    public FeeConfigurationService(RegionalBaseFeeRepository baseFees,
            TemperatureFeeRuleRepository temperatures, WindFeeRuleRepository winds,
            PhenomenonFeeRuleRepository phenomena, PlatformTransactionManager manager) {
        this.baseFees = baseFees;
        this.temperatures = temperatures;
        this.winds = winds;
        this.phenomena = phenomena;
        this.transactions = new TransactionTemplate(manager);
    }

    /** Returns all regional base fees ordered by ID. */
    public List<RegionalBaseFee> baseFees() {
        return baseFees.findAll(ID_ORDER);
    }

    /** Returns all temperature rules ordered by ID. */
    public List<TemperatureFeeRule> temperatureRules() {
        return temperatures.findAll(ID_ORDER);
    }

    /** Returns all wind rules ordered by ID. */
    public List<WindFeeRule> windRules() {
        return winds.findAll(ID_ORDER);
    }

    /** Returns all phenomenon rules ordered by ID. */
    public List<PhenomenonFeeRule> phenomenonRules() {
        return phenomena.findAll(ID_ORDER);
    }

    /**
     * Creates a base fee for a city and vehicle pair. The pair must be unique and the amount
     * must be a valid non-negative euro amount.
     *
     * @throws ConfigurationException for invalid input or an existing city and vehicle pair
     */
    public RegionalBaseFee createBase(City city, VehicleType vehicle, BigDecimal amount) {
        return write(() -> {
            requiredEnums(city, vehicle);
            validateAmount(amount);
            if (baseFees.findByCityAndVehicleType(city, vehicle).isPresent()) {
                throw conflict("Base fee already exists for city and vehicle type");
            }
            return baseFees.saveAndFlush(new RegionalBaseFee(city, vehicle, amount));
        });
    }

    /**
     * Replaces every field of a base fee, preserving its ID. The new city and vehicle pair
     * must remain unique.
     *
     * @throws ConfigurationException for invalid input, an unknown ID, or a duplicate pair
     */
    public RegionalBaseFee updateBase(long id, City city, VehicleType vehicle, BigDecimal amount) {
        return write(() -> {
            requiredEnums(city, vehicle);
            validateAmount(amount);
            RegionalBaseFee current = required(baseFees, id);
            baseFees.findByCityAndVehicleType(city, vehicle)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw conflict("Base fee already exists for city and vehicle type");
                    });
            current.replace(city, vehicle, amount);
            return baseFees.saveAndFlush(current);
        });
    }

    /**
     * Creates a temperature surcharge range for a vehicle. At least one bound is required;
     * null bounds are unbounded. The range must be nonempty and cannot overlap another
     * temperature range for the same vehicle, accounting for endpoint inclusivity.
     *
     * @throws ConfigurationException for invalid input or an overlapping range
     */
    public TemperatureFeeRule createTemperature(VehicleType vehicle, BigDecimal lower,
            BigDecimal upper, boolean lowerInclusive, boolean upperInclusive, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle);
            validateAmount(amount);
            validateBounds(lower, upper, lowerInclusive, upperInclusive);
            assertTemperatureNoOverlap(vehicle, lower, upper, lowerInclusive, upperInclusive, null);
            return temperatures.saveAndFlush(new TemperatureFeeRule(
                    vehicle, lower, upper, lowerInclusive, upperInclusive, amount));
        });
    }

    /**
     * Replaces a temperature rule while preserving its ID. The new range must be nonempty
     * and cannot overlap another temperature rule for its vehicle.
     *
     * @throws ConfigurationException for invalid input, an unknown ID, or an overlapping range
     */
    public TemperatureFeeRule updateTemperature(long id, VehicleType vehicle, BigDecimal lower,
            BigDecimal upper, boolean lowerInclusive, boolean upperInclusive, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle);
            validateAmount(amount);
            validateBounds(lower, upper, lowerInclusive, upperInclusive);
            TemperatureFeeRule current = required(temperatures, id);
            assertTemperatureNoOverlap(vehicle, lower, upper, lowerInclusive, upperInclusive, id);
            current.replace(vehicle, lower, upper, lowerInclusive, upperInclusive, amount);
            return temperatures.saveAndFlush(current);
        });
    }

    /**
     * Creates a wind-speed range for a vehicle. At least one bound is required; null bounds
     * are unbounded. The range must be nonempty and non-overlapping for that vehicle.
     * A forbidden outcome requires a zero euro amount.
     *
     * @throws ConfigurationException for invalid input or an overlapping range
     */
    public WindFeeRule createWind(VehicleType vehicle, BigDecimal lower, BigDecimal upper,
            boolean lowerInclusive, boolean upperInclusive, RuleOutcome outcome, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle);
            validateOutcome(outcome, amount);
            validateBounds(lower, upper, lowerInclusive, upperInclusive);
            assertWindNoOverlap(vehicle, lower, upper, lowerInclusive, upperInclusive, null);
            return winds.saveAndFlush(new WindFeeRule(
                    vehicle, lower, upper, lowerInclusive, upperInclusive, outcome, amount));
        });
    }

    /**
     * Replaces a wind rule while preserving its ID. The range and outcome must satisfy the
     * same constraints as a newly created wind rule.
     *
     * @throws ConfigurationException for invalid input, an unknown ID, or an overlapping range
     */
    public WindFeeRule updateWind(long id, VehicleType vehicle, BigDecimal lower, BigDecimal upper,
            boolean lowerInclusive, boolean upperInclusive, RuleOutcome outcome, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle);
            validateOutcome(outcome, amount);
            validateBounds(lower, upper, lowerInclusive, upperInclusive);
            WindFeeRule current = required(winds, id);
            assertWindNoOverlap(vehicle, lower, upper, lowerInclusive, upperInclusive, id);
            current.replace(vehicle, lower, upper, lowerInclusive, upperInclusive, outcome, amount);
            return winds.saveAndFlush(current);
        });
    }

    /**
     * Creates a rule for one vehicle and phenomenon pair. The pair must be unique;
     * a forbidden outcome requires a zero euro amount.
     *
     * @throws ConfigurationException for invalid input or a duplicate pair
     */
    public PhenomenonFeeRule createPhenomenon(VehicleType vehicle, WeatherPhenomenon phenomenon,
            RuleOutcome outcome, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle, phenomenon);
            validateOutcome(outcome, amount);
            if (phenomena.findAllByVehicleType(vehicle).stream()
                    .anyMatch(existing -> existing.getPhenomenon() == phenomenon)) {
                throw conflict("Phenomenon rule already exists for vehicle type");
            }
            return phenomena.saveAndFlush(new PhenomenonFeeRule(vehicle, phenomenon, outcome, amount));
        });
    }

    /**
     * Replaces a phenomenon rule while preserving its ID. The resulting vehicle and
     * phenomenon pair must be unique.
     *
     * @throws ConfigurationException for invalid input, an unknown ID, or a duplicate pair
     */
    public PhenomenonFeeRule updatePhenomenon(long id, VehicleType vehicle,
            WeatherPhenomenon phenomenon, RuleOutcome outcome, BigDecimal amount) {
        return write(() -> {
            requiredEnums(vehicle, phenomenon);
            validateOutcome(outcome, amount);
            PhenomenonFeeRule current = required(phenomena, id);
            if (phenomena.findAllByVehicleType(vehicle).stream()
                    .anyMatch(existing -> existing.getPhenomenon() == phenomenon
                            && !existing.getId().equals(id))) {
                throw conflict("Phenomenon rule already exists for vehicle type");
            }
            current.replace(vehicle, phenomenon, outcome, amount);
            return phenomena.saveAndFlush(current);
        });
    }

    /** Returns a base fee by ID, or throws a status-404 {@link ConfigurationException}. */
    public RegionalBaseFee getBase(long id) {
        return required(baseFees, id);
    }

    /** Returns a temperature rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public TemperatureFeeRule getTemperature(long id) {
        return required(temperatures, id);
    }

    /** Returns a wind rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public WindFeeRule getWind(long id) {
        return required(winds, id);
    }

    /** Returns a phenomenon rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public PhenomenonFeeRule getPhenomenon(long id) {
        return required(phenomena, id);
    }

    /** Deletes a base fee by ID, or throws a status-404 {@link ConfigurationException}. */
    public void deleteBase(long id) {
        deleteEntity(baseFees, id);
    }

    /** Deletes a temperature rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public void deleteTemperature(long id) {
        deleteEntity(temperatures, id);
    }

    /** Deletes a wind rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public void deleteWind(long id) {
        deleteEntity(winds, id);
    }

    /** Deletes a phenomenon rule by ID, or throws a status-404 {@link ConfigurationException}. */
    public void deletePhenomenon(long id) {
        deleteEntity(phenomena, id);
    }

    private void assertTemperatureNoOverlap(VehicleType vehicle, BigDecimal lower, BigDecimal upper,
            boolean lowerInclusive, boolean upperInclusive, Long excludedId) {
        for (TemperatureFeeRule existing : temperatures.findAllByVehicleType(vehicle)) {
            if (!existing.getId().equals(excludedId)
                    && overlaps(lower, upper, lowerInclusive, upperInclusive,
                            existing.getLowerThreshold(), existing.getUpperThreshold(),
                            existing.isLowerInclusive(), existing.isUpperInclusive())) {
                throw conflict("Temperature rule range overlaps an existing rule");
            }
        }
    }

    private void assertWindNoOverlap(VehicleType vehicle, BigDecimal lower, BigDecimal upper,
            boolean lowerInclusive, boolean upperInclusive, Long excludedId) {
        for (WindFeeRule existing : winds.findAllByVehicleType(vehicle)) {
            if (!existing.getId().equals(excludedId)
                    && overlaps(lower, upper, lowerInclusive, upperInclusive,
                            existing.getLowerThreshold(), existing.getUpperThreshold(),
                            existing.isLowerInclusive(), existing.isUpperInclusive())) {
                throw conflict("Wind rule range overlaps an existing rule");
            }
        }
    }

    private boolean overlaps(BigDecimal firstLower, BigDecimal firstUpper,
            boolean firstLowerInclusive, boolean firstUpperInclusive,
            BigDecimal secondLower, BigDecimal secondUpper,
            boolean secondLowerInclusive, boolean secondUpperInclusive) {
        if (firstUpper != null && secondLower != null) {
            int comparison = firstUpper.compareTo(secondLower);
            if (comparison < 0 || comparison == 0 && !(firstUpperInclusive && secondLowerInclusive)) {
                return false;
            }
        }
        if (secondUpper != null && firstLower != null) {
            int comparison = secondUpper.compareTo(firstLower);
            return comparison >= 0 && (comparison != 0 || secondUpperInclusive && firstLowerInclusive);
        }
        return true;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || exceeds(amount, 8, 2)) {
            throw bad("feeAmount must be non-negative and fit DECIMAL(10,2)");
        }
    }

    private void validateOutcome(RuleOutcome outcome, BigDecimal amount) {
        validateAmount(amount);
        if (outcome == null) {
            throw bad("outcome is required");
        }
        if (outcome == RuleOutcome.FORBIDDEN && amount.signum() != 0) {
            throw bad("FORBIDDEN rules must have feeAmount 0");
        }
    }

    private void validateBounds(BigDecimal lower, BigDecimal upper,
            boolean lowerInclusive, boolean upperInclusive) {
        if (lower == null && upper == null) {
            throw bad("At least one threshold must be present");
        }
        if (lower != null && exceeds(lower, 5, 3) || upper != null && exceeds(upper, 5, 3)) {
            throw bad("Thresholds must fit DECIMAL(8,3)");
        }
        if (lower != null && upper != null
                && (lower.compareTo(upper) > 0
                        || lower.compareTo(upper) == 0 && !(lowerInclusive && upperInclusive))) {
            throw bad("Invalid threshold bounds");
        }
    }

    private boolean exceeds(BigDecimal value, int maxIntegerDigits, int maxScale) {
        BigDecimal normalized = value.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        return scale > maxScale || integerDigits > maxIntegerDigits;
    }

    private <T> T required(JpaRepository<T, Long> repository, long id) {
        return repository.findById(id).orElseThrow(this::missing);
    }

    private void requiredEnums(Object... values) {
        for (Object value : values) {
            if (value == null) {
                throw bad("Required fields must be provided");
            }
        }
    }

    private <T> T write(Supplier<T> work) {
        synchronized (writeLock) {
            return transactions.execute(status -> work.get());
        }
    }

    private <T> void deleteEntity(JpaRepository<T, Long> repository, long id) {
        write(() -> {
            repository.delete(required(repository, id));
            repository.flush();
            return null;
        });
    }

    private ConfigurationException bad(String message) {
        return new ConfigurationException(400, message);
    }

    private ConfigurationException conflict(String message) {
        return new ConfigurationException(409, message);
    }

    private ConfigurationException missing() {
        return new ConfigurationException(404, "Configuration not found");
    }
}
