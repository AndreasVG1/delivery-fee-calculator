package andvil.feeCalculator.fee;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Selects active fee rules and validates weather restrictions and threshold coverage. */
@Service
public class FeeConfigurationService {
    private static final String FORBIDDEN = "Usage of selected vehicle type is forbidden";
    private static final String UNCOVERED = "Uncovered range";
    private final FeeConfigurationRepository repository;

    /** Creates the service with read-only access to fee configuration. */
    public FeeConfigurationService(FeeConfigurationRepository repository) { this.repository = repository; }

    /** Resolves all component fees for the request and observation or throws a business error. */
    public ResolvedFees resolve(City city, VehicleType vehicle, BigDecimal temperature,
            BigDecimal windSpeed, String providerPhenomenon) {
        RegionalBaseFee baseFee = repository.findBaseFee(city, vehicle).orElse(null);
        TemperatureFeeRule temperatureRule = findTemperatureRule(repository.findTemperatureRules(vehicle), temperature);
        WindFeeRule windRule = findWindRule(repository.findWindRules(vehicle), windSpeed);
        WeatherPhenomenon phenomenon = WeatherPhenomenon.fromProviderValue(providerPhenomenon);
        PhenomenonFeeRule phenomenonRule = repository.findPhenomenonRules(vehicle).stream()
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
        List<TemperatureFeeRule> matching = rules.stream().filter(rule -> matches(rule, value)).toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private WindFeeRule findWindRule(List<WindFeeRule> rules, BigDecimal value) {
        List<WindFeeRule> matching = rules.stream().filter(rule -> matches(rule, value)).toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private boolean matches(TemperatureFeeRule rule, BigDecimal value) {
        return inRange(rule.getLowerThreshold(), rule.isLowerInclusive(), rule.getUpperThreshold(), rule.isUpperInclusive(), value);
    }
    private boolean matches(WindFeeRule rule, BigDecimal value) {
        return inRange(rule.getLowerThreshold(), rule.isLowerInclusive(), rule.getUpperThreshold(), rule.isUpperInclusive(), value);
    }
    private boolean inRange(BigDecimal lower, boolean lowerInclusive, BigDecimal upper, boolean upperInclusive, BigDecimal value) {
        boolean aboveLower = lower == null || value.compareTo(lower) > 0 || (lowerInclusive && value.compareTo(lower) == 0);
        boolean belowUpper = upper == null || value.compareTo(upper) < 0 || (upperInclusive && value.compareTo(upper) == 0);
        return aboveLower && belowUpper;
    }
}
