package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;
import java.util.List;

import andvil.feeCalculator.fee.model.City;
import andvil.feeCalculator.fee.model.PhenomenonFeeRule;
import andvil.feeCalculator.fee.model.RegionalBaseFee;
import andvil.feeCalculator.fee.model.RuleOutcome;
import andvil.feeCalculator.fee.model.TemperatureFeeRule;
import andvil.feeCalculator.fee.model.VehicleType;
import andvil.feeCalculator.fee.model.WeatherPhenomenon;
import andvil.feeCalculator.fee.model.WindFeeRule;
import andvil.feeCalculator.fee.persistence.PhenomenonFeeRuleRepository;
import andvil.feeCalculator.fee.persistence.RegionalBaseFeeRepository;
import andvil.feeCalculator.fee.persistence.TemperatureFeeRuleRepository;
import andvil.feeCalculator.fee.persistence.WindFeeRuleRepository;
import org.springframework.stereotype.Service;

/** Resolves the active fee rules for a delivery and reports uncovered or forbidden conditions. */
@Service
public class FeeRuleResolver {
    private static final String FORBIDDEN = "Usage of selected vehicle type is forbidden";
    private static final String UNCOVERED = "Uncovered range";

    private final RegionalBaseFeeRepository baseFees;
    private final TemperatureFeeRuleRepository temperatures;
    private final WindFeeRuleRepository winds;
    private final PhenomenonFeeRuleRepository phenomena;

    /** Creates a resolver backed by the currently stored base and weather fee rules. */
    public FeeRuleResolver(RegionalBaseFeeRepository baseFees,
            TemperatureFeeRuleRepository temperatures, WindFeeRuleRepository winds,
            PhenomenonFeeRuleRepository phenomena) {
        this.baseFees = baseFees;
        this.temperatures = temperatures;
        this.winds = winds;
        this.phenomena = phenomena;
    }

    /**
     * Resolves the four fee components using the rules currently stored for the vehicle.
     * A matching forbidden wind or phenomenon rule takes precedence over missing rule coverage.
     * Each temperature and wind value must match exactly one rule.
     *
     * @param temperature air temperature in degrees Celsius
     * @param windSpeed wind speed in metres per second
     * @param providerPhenomenon raw phenomenon text from the weather provider
     * @return the regional base fee and three weather surcharges, in euros
     * @throws FeeCalculationException when delivery is forbidden or any component lacks a unique matching rule
     */
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
}
