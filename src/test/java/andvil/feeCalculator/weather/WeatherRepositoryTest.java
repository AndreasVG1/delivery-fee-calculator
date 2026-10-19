package andvil.feeCalculator.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = "spring.sql.init.data-locations=classpath:/empty-data.sql")
class WeatherRepositoryTest {

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savedObservationCanBeRetrievedWithEveryWeatherFieldUnchanged() {
        WeatherObservation observation = new WeatherObservation(
                "Tallinn-Harku",
                "26038",
                new BigDecimal("-3.125"),
                new BigDecimal("4.750"),
                "Freezing drizzle",
                OffsetDateTime.parse("2025-01-15T09:30:00+02:00"));

        WeatherObservation saved = weatherRepository.saveAndFlush(observation);
        entityManager.clear();

        WeatherObservation retrieved = weatherRepository.findById(saved.getId()).orElseThrow();

        assertThat(retrieved.getId()).isEqualTo(saved.getId());
        assertThat(retrieved.getStationName()).isEqualTo("Tallinn-Harku");
        assertThat(retrieved.getWmoCode()).isEqualTo("26038");
        assertThat(retrieved.getAirTemperature()).isEqualByComparingTo("-3.125");
        assertThat(retrieved.getWindSpeed()).isEqualByComparingTo("4.750");
        assertThat(retrieved.getPhenomenon()).isEqualTo("Freezing drizzle");
        assertThat(retrieved.getObservedAt()).isEqualTo(OffsetDateTime.parse("2025-01-15T09:30:00+02:00"));
    }

    @Test
    void observationsAtDifferentTimesRemainAsSeparateHistoricalRecords() {
        WeatherObservation earlier = weatherRepository.save(new WeatherObservation(
                "Tallinn-Harku", "26038", new BigDecimal("1.000"), new BigDecimal("2.000"),
                "Clear", OffsetDateTime.parse("2025-01-15T08:00:00+02:00")));
        WeatherObservation later = weatherRepository.save(new WeatherObservation(
                "Tallinn-Harku", "26038", new BigDecimal("2.000"), new BigDecimal("3.000"),
                "Light rain", OffsetDateTime.parse("2025-01-15T09:00:00+02:00")));
        weatherRepository.flush();

        List<WeatherObservation> history = weatherRepository.findAllByWmoCodeOrderByObservedAtAsc("26038");

        assertThat(history).extracting(WeatherObservation::getId)
                .containsExactly(earlier.getId(), later.getId());
        assertThat(history).extracting(WeatherObservation::getPhenomenon)
                .containsExactly("Clear", "Light rain");
    }

}
