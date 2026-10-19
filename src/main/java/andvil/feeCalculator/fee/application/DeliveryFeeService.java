package andvil.feeCalculator.fee.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.NoSuchElementException;

import andvil.feeCalculator.fee.model.City;
import andvil.feeCalculator.fee.model.DeliveryFeeCalculation;
import andvil.feeCalculator.fee.model.VehicleType;
import andvil.feeCalculator.fee.persistence.DeliveryFeeCalculationRepository;
import andvil.feeCalculator.weather.model.WeatherObservation;
import andvil.feeCalculator.weather.persistence.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reuses eligible history or records a delivery fee calculation. */
@Service
public class DeliveryFeeService {
    private static final String NO_WEATHER = "No weather data available for the requested time";
    private static final Duration MAX_AGE = Duration.ofHours(24);
    private final WeatherRepository weatherRepository;
    private final FeeConfigurationService feeConfigurationService;
    private final FeeCalculator feeCalculator;
    private final DeliveryFeeCalculationRepository calculationRepository;

    /** Creates the fee service with observation lookup, rule resolution, calculation, and history storage. */
    public DeliveryFeeService(WeatherRepository weatherRepository, FeeConfigurationService feeConfigurationService,
            FeeCalculator feeCalculator, DeliveryFeeCalculationRepository calculationRepository) {
        this.weatherRepository = weatherRepository; this.feeConfigurationService = feeConfigurationService;
        this.feeCalculator = feeCalculator; this.calculationRepository = calculationRepository;
    }

    /** Reuses eligible historical results or saves a new calculation using nearby weather. */
    @Transactional
    public DeliveryFeeCalculation calculate(City city, VehicleType vehicle, OffsetDateTime requestedAt) {
        OffsetDateTime effectiveAt = requestedAt == null ? OffsetDateTime.now() : requestedAt;
        OffsetDateTime calculatedAt = OffsetDateTime.now();
        if (requestedAt != null) {
            DeliveryFeeCalculation reusable = calculationRepository.findSuccessfulInWindow(city, vehicle,
                            requestedAt.minus(MAX_AGE), requestedAt.plus(MAX_AGE)).stream()
                    .min(Comparator.comparing((DeliveryFeeCalculation c) -> absoluteDuration(
                                    c.getWeatherObservation().getObservedAt(), requestedAt))
                            .thenComparing(DeliveryFeeCalculation::getCalculatedAt, Comparator.reverseOrder())
                            .thenComparing(DeliveryFeeCalculation::getId))
                    .orElse(null);
            if (reusable != null) return reusable;
        }
        WeatherObservation observation = weatherRepository.findAllByStationNameAndObservedAtBetween(
                        city.getStationName(), effectiveAt.minus(MAX_AGE), effectiveAt.plus(MAX_AGE)).stream()
                .min(Comparator.comparing((WeatherObservation candidate) -> absoluteDuration(candidate.getObservedAt(), effectiveAt))
                        .thenComparing(WeatherObservation::getObservedAt).thenComparing(WeatherObservation::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (observation == null) return save(failed(requestedAt, calculatedAt, city, vehicle, null, NO_WEATHER));
        try {
            ResolvedFees fees = feeConfigurationService.resolve(city, vehicle, observation.getAirTemperature(),
                    observation.getWindSpeed(), observation.getPhenomenon());
            BigDecimal total = feeCalculator.calculate(fees.rbf(), fees.atef(), fees.wsef(), fees.wpef());
            return save(new DeliveryFeeCalculation(requestedAt, calculatedAt, city, vehicle, observation,
                    fees.rbf(), fees.atef(), fees.wsef(), fees.wpef(), total, null));
        } catch (FeeCalculationException exception) {
            return save(failed(requestedAt, calculatedAt, city, vehicle, observation, exception.getMessage()));
        }
    }

    /** Retrieves an immutable recorded calculation by its identifier. */
    @Transactional(readOnly = true)
    public DeliveryFeeCalculation getCalculation(long id) {
        return calculationRepository.findWithObservationById(id).orElseThrow(NoSuchElementException::new);
    }

    private DeliveryFeeCalculation save(DeliveryFeeCalculation calculation) {
        return calculationRepository.save(calculation);
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
