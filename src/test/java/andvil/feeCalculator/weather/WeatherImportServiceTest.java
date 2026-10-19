package andvil.feeCalculator.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = "spring.sql.init.data-locations=classpath:/empty-data.sql")
class WeatherImportServiceTest {

    @Autowired
    private WeatherRepository weatherRepository;

    @Test
    void importLatestPersistsMappedWeatherFieldsAndReturnsNumberOfNewRows() {
        String xml = """
                <observations timestamp="1736933400">
                  <station><name>Tallinn-Harku</name><wmocode>26038</wmocode>
                    <airtemperature>-3.6</airtemperature><windspeed>3.2</windspeed>
                    <phenomenon>Light snowfall</phenomenon></station>
                </observations>
                """;
        WeatherImportService service = serviceReturning(xml);

        int imported = service.importLatest();

        assertThat(imported).isEqualTo(1);
        WeatherObservation saved = weatherRepository.findAll().getFirst();
        assertThat(saved.getStationName()).isEqualTo("Tallinn-Harku");
        assertThat(saved.getWmoCode()).isEqualTo("26038");
        assertThat(saved.getAirTemperature()).isEqualByComparingTo(new BigDecimal("-3.6"));
        assertThat(saved.getWindSpeed()).isEqualByComparingTo(new BigDecimal("3.2"));
        assertThat(saved.getPhenomenon()).isEqualTo("Light snowfall");
        assertThat(saved.getObservedAt()).isEqualTo(OffsetDateTime.parse("2025-01-15T09:30:00Z"));
    }

    @Test
    void importLatestFiltersStationsAndReportsIncompleteMappedStations() {
        String xml = """
                <observations timestamp="1736933400">
                  <station><name>Tallinn-Harku</name><wmocode>26038</wmocode><airtemperature>-3.6</airtemperature><windspeed>3.2</windspeed><phenomenon>Clear</phenomenon></station>
                  <station><name>Tartu-Tõravere</name><wmocode>26242</wmocode><airtemperature>bad</airtemperature><windspeed>3.2</windspeed><phenomenon>Clear</phenomenon></station>
                  <station><name>Pärnu</name><wmocode>26142</wmocode><airtemperature>1</airtemperature><windspeed>4</windspeed><phenomenon>Light rain</phenomenon></station>
                  <station><name>Virtsu</name><wmocode>26128</wmocode><airtemperature>1</airtemperature><windspeed>4</windspeed><phenomenon>Clear</phenomenon></station>
                </observations>
                """;

        Logger logger = (Logger) LoggerFactory.getLogger(WeatherImportService.class);
        ListAppender<ILoggingEvent> warnings = new ListAppender<>();
        warnings.start();
        logger.addAppender(warnings);
        try {
            assertThat(serviceReturning(xml).importLatest()).isEqualTo(2);
            assertThat(weatherRepository.findAll()).extracting(WeatherObservation::getStationName)
                    .containsExactlyInAnyOrder("Tallinn-Harku", "Pärnu");
            assertThat(warnings.list).extracting(ILoggingEvent::getFormattedMessage)
                    .contains("Skipping weather station Tartu-Tõravere: required observation data is empty or invalid");
        } finally {
            logger.detachAppender(warnings);
            warnings.stop();
        }
    }

    @Test
    void importLatestKeepsExistingObservationWhenItsStationAndTimeRecur() {
        String original = singleStationXml("Clear");
        String changed = singleStationXml("Light rain");
        assertThat(serviceReturning(original).importLatest()).isEqualTo(1);

        assertThat(serviceReturning(changed).importLatest()).isZero();

        List<WeatherObservation> observations = weatherRepository.findAll();
        assertThat(observations).hasSize(1);
        assertThat(observations.getFirst().getPhenomenon()).isEqualTo("Clear");
    }

    @Test
    void importLatestStoresNothingWhenFeedIsMalformed() {
        String malformed = "<observations timestamp=\"1736933400\"><station>";

        assertThatThrownBy(() -> serviceReturning(malformed).importLatest())
                .isInstanceOf(WeatherImportException.class);

        assertThat(weatherRepository.count()).isZero();
    }

    @Test
    void importLatestRejectsAnUnrelatedXmlDocument() {
        String unrelated = """
                <other timestamp="1736933400">
                  <station><name>Tallinn-Harku</name><wmocode>26038</wmocode><airtemperature>-3.6</airtemperature><windspeed>3.2</windspeed><phenomenon>Clear</phenomenon></station>
                </other>
                """;

        assertThatThrownBy(() -> serviceReturning(unrelated).importLatest())
                .isInstanceOf(WeatherImportException.class);
        assertThat(weatherRepository.count()).isZero();
    }

    @Test
    void importLatestStoresNothingWhenThePortalRequestFails() {
        WeatherClient client = new WeatherClient(URI.create("http://localhost"),
                java.time.Duration.ofMillis(1), java.time.Duration.ofMillis(1)) {
            @Override
            public String fetchLatestXml() {
                throw new WeatherClientException("portal unavailable");
            }
        };

        assertThatThrownBy(() -> new WeatherImportService(client, weatherRepository).importLatest())
                .isInstanceOf(WeatherClientException.class);
        assertThat(weatherRepository.count()).isZero();
    }

    private String singleStationXml(String phenomenon) {
        return """
                <observations timestamp="1736933400">
                  <station><name>Tallinn-Harku</name><wmocode>26038</wmocode><airtemperature>-3.6</airtemperature><windspeed>3.2</windspeed><phenomenon>%s</phenomenon></station>
                </observations>
                """.formatted(phenomenon);
    }

    private WeatherImportService serviceReturning(String xml) {
        WeatherClient client = new WeatherClient(URI.create("http://localhost"),
                java.time.Duration.ofMillis(1), java.time.Duration.ofMillis(1)) {
            @Override
            public String fetchLatestXml() {
                return xml;
            }
        };
        return new WeatherImportService(client, weatherRepository);
    }
}
