package andvil.feeCalculator.fee;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Resolves delivery fees and manages validated fee configuration. */
@Service
public class FeeConfigurationService {
    private static final String FORBIDDEN = "Usage of selected vehicle type is forbidden";
    private static final String UNCOVERED = "Uncovered range";
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

    /** Resolves all component fees for the request or throws a business error. */
    public ResolvedFees resolve(City city, VehicleType vehicle, BigDecimal temperature,
            BigDecimal windSpeed, String providerPhenomenon) {
        RegionalBaseFee baseFee = baseFees.findByCityAndVehicleType(city, vehicle).orElse(null);
        TemperatureFeeRule temperatureRule = findTemperatureRule(
                temperatures.findAllByVehicleType(vehicle), temperature);
        WindFeeRule windRule = findWindRule(winds.findAllByVehicleType(vehicle), windSpeed);
        WeatherPhenomenon phenomenon = WeatherPhenomenon.fromProviderValue(providerPhenomenon);
        PhenomenonFeeRule phenomenonRule = phenomena.findAllByVehicleType(vehicle).stream()
                .filter(rule -> rule.getPhenomenon() == phenomenon).findFirst().orElse(null);

        if ((windRule != null && windRule.getOutcome() == RuleOutcome.FORBIDDEN)
                || (phenomenonRule != null && phenomenonRule.getOutcome() == RuleOutcome.FORBIDDEN)) {
            throw new FeeCalculationException(FORBIDDEN);
        }
        if (baseFee == null || temperatureRule == null || windRule == null || phenomenonRule == null) {
            throw new FeeCalculationException(UNCOVERED);
        }
        return new ResolvedFees(baseFee.getFeeAmount(), temperatureRule.getFeeAmount(),
                windRule.getFeeAmount(), phenomenonRule.getFeeAmount());
    }

    /** Lists all regional base fees. */
    public List<RegionalBaseFee> baseFees() {
        return baseFees.findAll(ID_ORDER);
    }

    /** Lists all temperature rules. */
    public List<TemperatureFeeRule> temperatureRules() {
        return temperatures.findAll(ID_ORDER);
    }

    /** Lists all wind rules. */
    public List<WindFeeRule> windRules() {
        return winds.findAll(ID_ORDER);
    }

    /** Lists all phenomenon rules. */
    public List<PhenomenonFeeRule> phenomenonRules() {
        return phenomena.findAll(ID_ORDER);
    }

    /** Creates a validated regional base fee. */
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

    /** Replaces every field of a regional base fee. */
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

    /** Creates a validated temperature fee rule. */
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

    /** Replaces every field of a temperature rule and checks its range. */
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

    /** Creates a validated wind fee rule. */
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

    /** Replaces every field of a wind rule and checks its range. */
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

    /** Creates a unique phenomenon rule for the vehicle. */
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

    /** Replaces every field of a phenomenon rule. */
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

    /** Returns a base fee or reports that its ID is missing. */
    public RegionalBaseFee getBase(long id) {
        return required(baseFees, id);
    }

    /** Returns a temperature rule or reports that its ID is missing. */
    public TemperatureFeeRule getTemperature(long id) {
        return required(temperatures, id);
    }

    /** Returns a wind rule or reports that its ID is missing. */
    public WindFeeRule getWind(long id) {
        return required(winds, id);
    }

    /** Returns a phenomenon rule or reports that its ID is missing. */
    public PhenomenonFeeRule getPhenomenon(long id) {
        return required(phenomena, id);
    }

    /** Deletes a base fee by ID. */
    public void deleteBase(long id) {
        deleteEntity(baseFees, id);
    }

    /** Deletes a temperature rule by ID. */
    public void deleteTemperature(long id) {
        deleteEntity(temperatures, id);
    }

    /** Deletes a wind rule by ID. */
    public void deleteWind(long id) {
        deleteEntity(winds, id);
    }

    /** Deletes a phenomenon rule by ID. */
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

    private TemperatureFeeRule findTemperatureRule(List<TemperatureFeeRule> rules, BigDecimal value) {
        List<TemperatureFeeRule> matching = rules.stream()
                .filter(rule -> matches(rule.getLowerThreshold(), rule.isLowerInclusive(),
                        rule.getUpperThreshold(), rule.isUpperInclusive(), value))
                .toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private WindFeeRule findWindRule(List<WindFeeRule> rules, BigDecimal value) {
        List<WindFeeRule> matching = rules.stream()
                .filter(rule -> matches(rule.getLowerThreshold(), rule.isLowerInclusive(),
                        rule.getUpperThreshold(), rule.isUpperInclusive(), value))
                .toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private boolean matches(BigDecimal lower, boolean lowerInclusive, BigDecimal upper,
            boolean upperInclusive, BigDecimal value) {
        boolean aboveLower = lower == null || value.compareTo(lower) > 0
                || lowerInclusive && value.compareTo(lower) == 0;
        boolean belowUpper = upper == null || value.compareTo(upper) < 0
                || upperInclusive && value.compareTo(upper) == 0;
        return aboveLower && belowUpper;
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
            if (comparison < 0 || comparison == 0 && !(secondUpperInclusive && firstLowerInclusive)) {
                return false;
            }
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
