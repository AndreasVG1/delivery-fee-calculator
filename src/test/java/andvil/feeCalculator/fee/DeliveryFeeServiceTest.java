package andvil.feeCalculator.fee;

import static org.assertj.core.api.Assertions.assertThat;

import andvil.feeCalculator.weather.WeatherObservation;
import andvil.feeCalculator.weather.WeatherRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class DeliveryFeeServiceTest {
    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2025-01-15T12:00:00Z");
    @Autowired private DeliveryFeeService service;
    @Autowired private WeatherRepository weatherRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    private int nextWmoCode;

    @BeforeEach
    void clearWeather() { jdbcTemplate.update("delete from delivery_fee_calculation"); weatherRepository.deleteAll(); weatherRepository.flush(); nextWmoCode = 0; }

    @Test
    void calculatesCombinedFeesFromCityVehicleAndObservedWeather() {
        addWeather("Tartu-Tõravere", "-12", "5", "Light snowfall", REQUESTED_AT);

        DeliveryFeeCalculation result = service.calculate(City.TARTU, VehicleType.SCOOTER, REQUESTED_AT);

        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getRbf()).isEqualByComparingTo("3.00");
        assertThat(result.getAtef()).isEqualByComparingTo("1.00");
        assertThat(result.getWsef()).isEqualByComparingTo("0.00");
        assertThat(result.getWpef()).isEqualByComparingTo("1.00");
        assertThat(result.getTotalFee()).isEqualByComparingTo("5.00");
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void appliesTemperatureFeesAtTheMinusTenAndZeroBoundaries() {
        addWeather("Tallinn-Harku", "-10", "0", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation atMinusTen = service.calculate(City.TALLINN, VehicleType.BIKE, REQUESTED_AT);
        assertThat(atMinusTen.getAtef()).isEqualByComparingTo("0.50");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "0", "0", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation atZero = service.calculate(City.TALLINN, VehicleType.BIKE, REQUESTED_AT);
        assertThat(atZero.getAtef()).isEqualByComparingTo("0.00");
    }

    @Test
    void appliesBikeWindSurchargeAtBothInclusiveBoundariesAndForbidsAboveTwenty() {
        addWeather("Pärnu", "5", "10", "Clear", REQUESTED_AT);
        assertThat(service.calculate(City.PÄRNU, VehicleType.BIKE, REQUESTED_AT).getWsef())
                .isEqualByComparingTo("0.50");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Pärnu", "5", "20", "Clear", REQUESTED_AT);
        assertThat(service.calculate(City.PÄRNU, VehicleType.BIKE, REQUESTED_AT).getWsef())
                .isEqualByComparingTo("0.50");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Pärnu", "5", "20.001", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation forbidden = service.calculate(City.PÄRNU, VehicleType.BIKE, REQUESTED_AT);
        assertThat(forbidden.getErrorMessage()).isEqualTo("Usage of selected vehicle type is forbidden");
        assertThat(forbidden.getTotalFee()).isNull();
        assertThat(forbidden.getRbf()).isNull();
        assertThat(forbidden.getWeatherObservation()).isNotNull();
    }

    @Test
    void appliesPhenomenonSurchargesAndRestrictions() {
        addWeather("Tallinn-Harku", "5", "0", "Light rain", REQUESTED_AT);
        assertThat(service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT).getWpef())
                .isEqualByComparingTo("0.50");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Light snow shower", REQUESTED_AT);
        assertThat(service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT).getWpef())
                .isEqualByComparingTo("1.00");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Light shower", REQUESTED_AT);
        assertThat(service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT).getWpef())
                .isEqualByComparingTo("0.50");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Unexpected rain advisory", REQUESTED_AT);
        assertThat(service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT).getWpef())
                .isEqualByComparingTo("0.00");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Hail", REQUESTED_AT);
        DeliveryFeeCalculation hail = service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT);
        assertThat(hail.getErrorMessage()).isEqualTo("Usage of selected vehicle type is forbidden");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Glaze", REQUESTED_AT);
        DeliveryFeeCalculation forbidden = service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT);
        assertThat(forbidden.getErrorMessage()).isEqualTo("Usage of selected vehicle type is forbidden");

        clearHistory(); weatherRepository.deleteAll();
        addWeather("Tallinn-Harku", "5", "0", "Thunderstorm", REQUESTED_AT);
        assertThat(service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT).getErrorMessage())
                .isEqualTo("Usage of selected vehicle type is forbidden");
    }

    @Test
    void choosesNearestObservationAndAcceptsTheExactTwentyFourHourLimit() {
        addWeather("Tallinn-Harku", "-12", "0", "Light snow", REQUESTED_AT.minusHours(23));
        WeatherObservation nearest = addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT.plusHours(2));

        DeliveryFeeCalculation result = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);

        assertThat(result.getWeatherObservation().getId()).isEqualTo(nearest.getId());
        assertThat(result.getTotalFee()).isEqualByComparingTo("4.00");

        clearHistory(); weatherRepository.deleteAll();
        WeatherObservation boundary = addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT.plusHours(24));
        assertThat(service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT).getWeatherObservation().getId())
                .isEqualTo(boundary.getId());
    }

    @Test
    void treatsNullRequestedTimeAsCurrentTime() {
        OffsetDateTime recentObservation = OffsetDateTime.now().minusHours(1);
        WeatherObservation observation = addWeather("Tallinn-Harku", "5", "0", "Clear", recentObservation);

        DeliveryFeeCalculation result = service.calculate(City.TALLINN, VehicleType.CAR, null);

        assertThat(result.getRequestedAt()).isNull();
        assertThat(result.getWeatherObservation().getId()).isEqualTo(observation.getId());
        assertThat(result.getTotalFee()).isEqualByComparingTo("4.00");
        assertThat(service.calculate(City.TALLINN, VehicleType.CAR, null).getId()).isNotEqualTo(result.getId());
    }

    @Test
    void reusesSuccessfulHistoryAtTheInclusiveTwentyFourHourBoundaryButNotBeyondIt() {
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation saved = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);

        DeliveryFeeCalculation boundary = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT.plusHours(24));
        DeliveryFeeCalculation outside = service.calculate(City.TALLINN, VehicleType.CAR,
                REQUESTED_AT.plusHours(24).plusNanos(1_000));

        assertThat(boundary.getId()).isEqualTo(saved.getId());
        assertThat(outside.getId()).isNotEqualTo(saved.getId());
        assertThat(outside.getErrorMessage()).isEqualTo("No weather data available for the requested time");
    }

    @Test
    void doesNotReuseHistoryForAnotherVehicleType() {
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation car = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);

        DeliveryFeeCalculation bike = service.calculate(City.TALLINN, VehicleType.BIKE, REQUESTED_AT);

        assertThat(bike.getId()).isNotEqualTo(car.getId());
        assertThat(bike.getVehicleType()).isEqualTo(VehicleType.BIKE);
    }

    @Test
    void doesNotReuseFailedHistoryOrHistoryForAnotherCity() {
        DeliveryFeeCalculation failed = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT);
        DeliveryFeeCalculation nowSuccessful = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);
        OffsetDateTime cityCheckAt = REQUESTED_AT.plusDays(2);
        addWeather("Tartu-Tõravere", "5", "0", "Clear", cityCheckAt);
        DeliveryFeeCalculation tartu = service.calculate(City.TARTU, VehicleType.CAR, cityCheckAt);
        DeliveryFeeCalculation otherCity = service.calculate(City.TALLINN, VehicleType.CAR, cityCheckAt);

        assertThat(failed.getErrorMessage()).isNotNull();
        assertThat(nowSuccessful.getErrorMessage()).isNull();
        assertThat(nowSuccessful.getId()).isNotEqualTo(failed.getId());
        assertThat(tartu.getErrorMessage()).isNull();
        assertThat(otherCity.getErrorMessage()).isEqualTo("No weather data available for the requested time");
        assertThat(otherCity.getId()).isNotEqualTo(tartu.getId());
    }

    @Test
    void selectsNearestSavedObservationAndUsesNewestCalculationForAnEquidistantTie() {
        addWeather("Tallinn-Harku", "-12", "0", "Clear", REQUESTED_AT.minusHours(20));
        DeliveryFeeCalculation earlier = service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT.minusHours(20));
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT.plusHours(20));
        DeliveryFeeCalculation later = service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT.plusHours(20));

        DeliveryFeeCalculation tied = service.calculate(City.TALLINN, VehicleType.SCOOTER, REQUESTED_AT);

        assertThat(earlier.getId()).isNotEqualTo(later.getId());
        assertThat(tied.getId()).isEqualTo(later.getId());
        assertThat(tied.getTotalFee()).isEqualByComparingTo("3.50");
    }

    @Test
    void returnsAnErrorWithoutFeeWhenNoObservationIsWithinTwentyFourHours() {
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT.plusHours(24).plusNanos(1_000_000));

        DeliveryFeeCalculation result = service.calculate(City.TALLINN, VehicleType.CAR, REQUESTED_AT);

        assertThat(result.getErrorMessage()).isEqualTo("No weather data available for the requested time");
        assertThat(result.getWeatherObservation()).isNull();
        assertThat(result.getTotalFee()).isNull();
        assertThat(result.getRbf()).isNull();
    }

    @Test
    void reportsUncoveredRangeWhenNoTemperatureRuleMatches() {
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT);
        jdbcTemplate.update("delete from temperature_fee_rule where vehicle_type = 'BIKE'");

        DeliveryFeeCalculation result = service.calculate(City.TALLINN, VehicleType.BIKE, REQUESTED_AT);

        assertThat(result.getErrorMessage()).isEqualTo("Uncovered range");
        assertThat(result.getWeatherObservation()).isNotNull();
        assertThat(result.getTotalFee()).isNull();
        assertThat(result.getAtef()).isNull();
    }

    @Test
    void reportsUncoveredRangeWhenMultipleTemperatureRulesMatch() {
        addWeather("Tallinn-Harku", "5", "0", "Clear", REQUESTED_AT);
        jdbcTemplate.update("""
                insert into temperature_fee_rule
                    (vehicle_type, lower_threshold, upper_threshold, lower_inclusive, upper_inclusive, fee_amount)
                values ('BIKE', 0, 10, true, true, 0)
                """);

        DeliveryFeeCalculation result = service.calculate(City.TALLINN, VehicleType.BIKE, REQUESTED_AT);

        assertThat(result.getErrorMessage()).isEqualTo("Uncovered range");
    }

    @ParameterizedTest
    @CsvSource({
            "TALLINN, Tallinn-Harku, CAR, 4.00", "TALLINN, Tallinn-Harku, SCOOTER, 3.50", "TALLINN, Tallinn-Harku, BIKE, 3.00",
            "TARTU, Tartu-Tõravere, CAR, 3.50", "TARTU, Tartu-Tõravere, SCOOTER, 3.00", "TARTU, Tartu-Tõravere, BIKE, 2.50",
            "PÄRNU, Pärnu, CAR, 3.00", "PÄRNU, Pärnu, SCOOTER, 2.50", "PÄRNU, Pärnu, BIKE, 2.00"
    })
    void usesTheCityAndVehicleSpecificRegionalBaseFee(City city, String station, VehicleType vehicle, String expected) {
        addWeather(station, "5", "0", "Clear", REQUESTED_AT);

        DeliveryFeeCalculation result = service.calculate(city, vehicle, REQUESTED_AT);

        assertThat(result.getRbf()).isEqualByComparingTo(expected);
    }

    private WeatherObservation addWeather(String station, String temperature, String wind, String phenomenon,
            OffsetDateTime observedAt) {
        return weatherRepository.saveAndFlush(new WeatherObservation(station, "test-" + (++nextWmoCode),
                new BigDecimal(temperature), new BigDecimal(wind), phenomenon, observedAt));
    }

    private void clearHistory() { jdbcTemplate.update("delete from delivery_fee_calculation"); entityManager.clear(); }
}
