package andvil.feeCalculator.fee;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import andvil.feeCalculator.weather.WeatherObservation;
import andvil.feeCalculator.weather.WeatherRepository;
import org.springframework.stereotype.Service;

/** Selects applicable weather and returns delivery fee calculations. */
@Service
public class DeliveryFeeService {
    private static final String NO_WEATHER = "No weather data available for the requested time";
    private static final Duration MAX_AGE = Duration.ofHours(24);
    private final WeatherRepository weatherRepository;
    private final FeeConfigurationService feeConfigurationService;
    private final FeeCalculator feeCalculator;

    /** Creates the fee service with observation lookup, rule resolution, and calculation. */
    public DeliveryFeeService(WeatherRepository weatherRepository, FeeConfigurationService feeConfigurationService,
            FeeCalculator feeCalculator) {
        this.weatherRepository = weatherRepository; this.feeConfigurationService = feeConfigurationService;
        this.feeCalculator = feeCalculator;
    }

    /** Calculates a fee for a city and vehicle using the nearest observation within 24 hours. */
    public DeliveryFeeCalculation calculate(City city, VehicleType vehicle, OffsetDateTime requestedAt) {
        OffsetDateTime effectiveAt = requestedAt == null ? OffsetDateTime.now() : requestedAt;
        OffsetDateTime calculatedAt = OffsetDateTime.now();
        WeatherObservation observation = weatherRepository.findAllByStationNameAndObservedAtBetween(
                        city.getStationName(), effectiveAt.minus(MAX_AGE), effectiveAt.plus(MAX_AGE)).stream()
                .min(Comparator.comparing((WeatherObservation candidate) -> absoluteDuration(candidate.getObservedAt(), effectiveAt))
                        .thenComparing(WeatherObservation::getObservedAt).thenComparing(WeatherObservation::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (observation == null) return failed(requestedAt, calculatedAt, city, vehicle, null, NO_WEATHER);
        try {
            ResolvedFees fees = feeConfigurationService.resolve(city, vehicle, observation.getAirTemperature(),
                    observation.getWindSpeed(), observation.getPhenomenon());
            BigDecimal total = feeCalculator.calculate(fees.rbf(), fees.atef(), fees.wsef(), fees.wpef());
            return new DeliveryFeeCalculation(requestedAt, calculatedAt, city, vehicle, observation,
                    fees.rbf(), fees.atef(), fees.wsef(), fees.wpef(), total, null);
        } catch (FeeCalculationException exception) {
            return failed(requestedAt, calculatedAt, city, vehicle, observation, exception.getMessage());
        }
    }

    private Duration absoluteDuration(OffsetDateTime first, OffsetDateTime second) {
        return Duration.between(first, second).abs();
    }
    private DeliveryFeeCalculation failed(OffsetDateTime requestedAt, OffsetDateTime calculatedAt, City city,
            VehicleType vehicle, WeatherObservation observation, String message) {
        return new DeliveryFeeCalculation(requestedAt, calculatedAt, city, vehicle, observation,
                null, null, null, null, null, message);
    }
}
