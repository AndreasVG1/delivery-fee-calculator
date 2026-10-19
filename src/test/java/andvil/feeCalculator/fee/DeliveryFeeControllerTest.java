package andvil.feeCalculator.fee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import andvil.feeCalculator.weather.WeatherObservation;
import andvil.feeCalculator.weather.WeatherRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeliveryFeeControllerTest {
    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2025-01-15T12:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private WeatherRepository weatherRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearWeather() {
        jdbcTemplate.update("delete from delivery_fee_calculation");
        weatherRepository.deleteAll();
        weatherRepository.flush();
    }

    @Test
    void returnsCalculatedFeeForExplicitDatetime() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tartu-Tõravere", "test-1",
                new BigDecimal("-12"), new BigDecimal("5"), "Light snowfall", REQUESTED_AT));

        mockMvc.perform(get("/api/delivery-fee")
                        .param("city", "Tartu")
                        .param("vehicleType", "scooter")
                        .param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.vehicleType").value("SCOOTER"))
                .andExpect(jsonPath("$.requestedAt").value("2025-01-15T12:00:00Z"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.totalFee").value(5.0));
    }

    @Test
    void retrievesSavedCalculationAndReturnsNotFoundForUnknownId() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tartu-Tõravere", "test-retrieval",
                new BigDecimal("-12"), new BigDecimal("5"), "Light snowfall", REQUESTED_AT));
        String body = mockMvc.perform(get("/api/delivery-fee").param("city", "Tartu")
                        .param("vehicleType", "scooter").param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/delivery-fee/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.requestedAt").value("2025-01-15T12:00:00Z"))
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.vehicleType").value("SCOOTER"))
                .andExpect(jsonPath("$.weatherObservationId").isNumber())
                .andExpect(jsonPath("$.totalFee").value(5.0));
        mockMvc.perform(get("/api/delivery-fee/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/delivery-fee/{id}", id)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/delivery-fee/{id}", id)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void returnsTheSameSavedIdAndAmountsWhenSuccessfulHistoryMatches() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "history-original",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", REQUESTED_AT));
        String first = mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "CAR")
                        .param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(first, "$.id")).longValue();
        jdbcTemplate.update("update regional_base_fee set fee_amount = 99 where city = 'TALLINN' and vehicle_type = 'CAR'");
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "history-newer-weather",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", REQUESTED_AT.plusMinutes(1)));

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "CAR")
                        .param("datetime", REQUESTED_AT.plusMinutes(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.totalFee").value(4.0));

        mockMvc.perform(get("/api/delivery-fee/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.calculatedAt").isNotEmpty())
                .andExpect(jsonPath("$.weatherObservationId").isNumber())
                .andExpect(jsonPath("$.rbf").value(4.0))
                .andExpect(jsonPath("$.atef").value(0.0))
                .andExpect(jsonPath("$.wsef").value(0.0))
                .andExpect(jsonPath("$.wpef").value(0.0))
                .andExpect(jsonPath("$.totalFee").value(4.0));
    }

    @Test
    void returnsCurrentConditionsWithNullRequestedAtWhenDatetimeIsOmitted() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "test-2",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", OffsetDateTime.now().minusHours(1)));

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "Car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedAt").value(nullValue()))
                .andExpect(jsonPath("$.totalFee").value(4.0));
    }

    @Test
    void acceptsCaseInsensitiveCityAndVehicleNamesIncludingAccentedParnu() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Pärnu", "test-3",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", OffsetDateTime.now().minusHours(1)));

        mockMvc.perform(get("/api/delivery-fee").param("city", "pärnu").param("vehicleType", "cAr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("PÄRNU"))
                .andExpect(jsonPath("$.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.totalFee").value(3.0));
    }

    @Test
    void returnsBadRequestWithErrorBodyForMissingOrMalformedInput() throws Exception {
        String invalidResponse = mockMvc.perform(get("/api/delivery-fee").param("vehicleType", "CAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        java.util.Map<?, ?> invalidBody = com.jayway.jsonpath.JsonPath.read(invalidResponse, "$");
        assertThat(invalidBody.containsKey("id")).isFalse();

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(get("/api/delivery-fee").param("city", "Riga").param("vehicleType", "CAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "TRUCK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn")
                        .param("vehicleType", "CAR").param("datetime", "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void returnsNotFoundWhenNoWeatherObservationIsAvailable() throws Exception {
        mockMvc.perform(get("/api/delivery-fee").param("city", "Tartu").param("vehicleType", "CAR")
                        .param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.error").value("No weather data available for the requested time"));
    }

    @Test
    void returnsUnprocessableEntityWhenWeatherForbidsTheSelectedVehicle() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "test-4",
                new BigDecimal("5"), new BigDecimal("0"), "Hail", REQUESTED_AT));

        String response = mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "SCOOTER")
                        .param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Usage of selected vehicle type is forbidden"))
                .andExpect(jsonPath("$.id").isNumber()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();
        mockMvc.perform(get("/api/delivery-fee/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.totalFee").value(nullValue()))
                .andExpect(jsonPath("$.errorMessage").value("Usage of selected vehicle type is forbidden"));
    }

    @Test
    void returnsUnprocessableEntityWhenFeeRulesDoNotCoverObservedWeather() throws Exception {
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "test-5",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", REQUESTED_AT));
        jdbcTemplate.update("delete from temperature_fee_rule where vehicle_type = 'BIKE'");

        mockMvc.perform(get("/api/delivery-fee").param("city", "Tallinn").param("vehicleType", "BIKE")
                        .param("datetime", REQUESTED_AT.toString()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Uncovered range"));
    }
}
