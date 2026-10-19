package andvil.feeCalculator.fee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import andvil.feeCalculator.weather.model.WeatherObservation;
import andvil.feeCalculator.weather.persistence.WeatherRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeeConfigurationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private WeatherRepository weatherRepository;

    @Test
    void regionalBaseFeesCanBeReadCreatedUpdatedAndDeleted() throws Exception {
        mockMvc.perform(get("/api/configurations/base-fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());

        mockMvc.perform(post("/api/configurations/base-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baseFeeJson("TALLINN", "CAR", "5.25")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/configurations/base-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baseFeeJson("TARTU", "CAR", "5.999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(delete("/api/configurations/base-fees/1"))
                .andExpect(status().isNoContent());

        MvcResult created = mockMvc.perform(post("/api/configurations/base-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baseFeeJson("TALLINN", "CAR", "5.25")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.feeAmount").value(5.25))
                .andReturn();
        long id = jsonLong(created, "id");

        mockMvc.perform(get("/api/configurations/base-fees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/configurations/base-fees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baseFeeJson("TALLINN", "CAR", "6.25")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeAmount").value(6.25));

        mockMvc.perform(delete("/api/configurations/base-fees/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/configurations/base-fees/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void temperatureRulesRejectInvalidOverlapAndSupportReplacementAndDeletion() throws Exception {
        mockMvc.perform(put("/api/configurations/temperature-rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", null, 0, false, true)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/configurations/temperature-rules/1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/configurations/temperature-rules/2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", null, null, false, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        MvcResult unbounded = mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", null, 5, false, false)))
                .andExpect(status().isCreated())
                .andReturn();
        mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", null, 4, false, false)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/configurations/temperature-rules/{id}", jsonLong(unbounded, "id")));

        MvcResult created = mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", 10, 20, true, true)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        long id = jsonLong(created, "id");
        mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", 20, 30, true, true)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", 20, 30, false, true)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/configurations/temperature-rules/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("CAR", 10, 19, true, true)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/configurations/temperature-rules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upperThreshold").value(19));
        mockMvc.perform(delete("/api/configurations/temperature-rules/{id}", id))
                .andExpect(status().isNoContent());

        for (long oldId : idsFor("/api/configurations/temperature-rules", "CAR")) {
            mockMvc.perform(delete("/api/configurations/temperature-rules/{id}", oldId));
        }
        for (boolean upperInclusive : new boolean[] {false, true}) {
            for (boolean lowerInclusive : new boolean[] {false, true}) {
                MvcResult base = mockMvc.perform(post("/api/configurations/temperature-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(temperatureJson("CAR", 10, 20, true, upperInclusive)))
                        .andExpect(status().isCreated())
                        .andReturn();
                long baseId = jsonLong(base, "id");
                MvcResult adjacent = mockMvc.perform(post("/api/configurations/temperature-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(temperatureJson("CAR", 20, 30, lowerInclusive, true)))
                        .andReturn();
                int expectedStatus = upperInclusive && lowerInclusive ? 409 : 201;
                assertEquals(expectedStatus, adjacent.getResponse().getStatus());
                if (expectedStatus == 201) {
                    mockMvc.perform(delete("/api/configurations/temperature-rules/{id}",
                                    jsonLong(adjacent, "id")));
                }
                mockMvc.perform(delete("/api/configurations/temperature-rules/{id}", baseId));
            }
        }
    }

    @Test
    void windRulesAndPhenomenonRulesSupportCrudAndValidation() throws Exception {
        List<Long> scooterIds = idsFor("/api/configurations/wind-rules", "SCOOTER");
        for (long id : scooterIds) {
            mockMvc.perform(delete("/api/configurations/wind-rules/{id}", id))
                    .andExpect(status().isNoContent());
        }

        MvcResult wind = mockMvc.perform(post("/api/configurations/wind-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(windJson(null, 7)))
                .andExpect(status().isCreated())
                .andReturn();
        long windId = jsonLong(wind, "id");
        mockMvc.perform(put("/api/configurations/wind-rules/{id}", windId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(windJson(null, 8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upperThreshold").value(8));
        mockMvc.perform(post("/api/configurations/wind-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(windJson(null, null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/configurations/wind-rules/{id}", windId))
                .andExpect(status().isNoContent());

        long rainId = phenomenonRuleId("CAR", "RAIN");
        mockMvc.perform(delete("/api/configurations/phenomenon-rules/{id}", rainId))
                .andExpect(status().isNoContent());
        MvcResult phenomenon = mockMvc.perform(post("/api/configurations/phenomenon-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phenomenonJson("CAR", "RAIN", "0.75")))
                .andExpect(status().isCreated())
                .andReturn();
        long phenomenonId = jsonLong(phenomenon, "id");
        mockMvc.perform(put("/api/configurations/phenomenon-rules/{id}", phenomenonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phenomenonJson("CAR", "RAIN", "0.80")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeAmount").value(0.80));
        mockMvc.perform(delete("/api/configurations/phenomenon-rules/{id}", phenomenonId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/configurations/base-fees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(post("/api/configurations/base-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\",\"feeAmount\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void concurrentWindCreatesAndUpdatesKeepRangesDisjoint() throws Exception {
        deleteRulesForVehicle("/api/configurations/wind-rules", "SCOOTER");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> firstCreate = () -> {
                start.await();
                return postWindRule(1, 10);
            };
            Callable<Integer> secondCreate = () -> {
                start.await();
                return postWindRule(5, 15);
            };
            Future<Integer> first = pool.submit(firstCreate);
            Future<Integer> second = pool.submit(secondCreate);
            start.countDown();
            List<Integer> createStatuses = List.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, createStatuses.stream().filter(status -> status == 201).count());
            assertEquals(1, createStatuses.stream().filter(status -> status == 409).count());

            JsonNode stored = jsonResponse("/api/configurations/wind-rules");
            JsonNode winner = onlyRuleForVehicle(stored, "SCOOTER");
            boolean rangeOneToTen = hasBounds(winner, 1, 10);
            boolean rangeFiveToFifteen = hasBounds(winner, 5, 15);
            assertTrue(rangeOneToTen || rangeFiveToFifteen);

            deleteRulesForVehicle("/api/configurations/wind-rules", "SCOOTER");
            postWindRuleExpecting(1, 5, 201);
            postWindRuleExpecting(20, 25, 201);
            List<Long> ids = idsFor("/api/configurations/wind-rules", "SCOOTER");
            long left = ids.get(0);
            long right = ids.get(1);
            CountDownLatch updateStart = new CountDownLatch(1);
            Callable<Integer> updateLeft = () -> {
                updateStart.await();
                return putWindRule(left, 1, 18);
            };
            Callable<Integer> updateRight = () -> {
                updateStart.await();
                return putWindRule(right, 10, 25);
            };
            Future<Integer> updateA = pool.submit(updateLeft);
            Future<Integer> updateB = pool.submit(updateRight);
            updateStart.countDown();
            List<Integer> updateStatuses = List.of(
                    updateA.get(20, TimeUnit.SECONDS), updateB.get(20, TimeUnit.SECONDS));
            assertEquals(1, updateStatuses.stream().filter(status -> status == 200).count());
            assertEquals(1, updateStatuses.stream().filter(status -> status == 409).count());

            JsonNode finalRules = jsonResponse("/api/configurations/wind-rules");
            if (updateStatuses.getFirst() == 200) {
                assertRange(finalRules, left, 1, 18);
                assertRange(finalRules, right, 20, 25);
            } else {
                assertRange(finalRules, left, 1, 5);
                assertRange(finalRules, right, 10, 25);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentTemperatureCreatesAndUpdatesKeepRangesDisjoint() throws Exception {
        deleteRulesForVehicle("/api/configurations/temperature-rules", "SCOOTER");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch createGate = new CountDownLatch(1);
            Callable<Integer> firstCreate = () -> {
                createGate.await();
                return postTemperatureRule(1, 10);
            };
            Callable<Integer> secondCreate = () -> {
                createGate.await();
                return postTemperatureRule(5, 15);
            };
            Future<Integer> first = pool.submit(firstCreate);
            Future<Integer> second = pool.submit(secondCreate);
            createGate.countDown();
            List<Integer> createStatuses = List.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, createStatuses.stream().filter(status -> status == 201).count());
            assertEquals(1, createStatuses.stream().filter(status -> status == 409).count());
            assertTrue(hasBounds(onlyRuleForVehicle(
                    jsonResponse("/api/configurations/temperature-rules"), "SCOOTER"), 1, 10)
                    || hasBounds(onlyRuleForVehicle(
                            jsonResponse("/api/configurations/temperature-rules"), "SCOOTER"), 5, 15));

            deleteRulesForVehicle("/api/configurations/temperature-rules", "SCOOTER");
            postTemperatureRuleExpecting(1, 5, 201);
            postTemperatureRuleExpecting(20, 25, 201);
            List<Long> ids = idsFor("/api/configurations/temperature-rules", "SCOOTER");
            long low = ids.get(0);
            long high = ids.get(1);
            CountDownLatch updateGate = new CountDownLatch(1);
            Callable<Integer> moveLow = () -> {
                updateGate.await();
                return putTemperatureRule(low, 1, 18);
            };
            Callable<Integer> moveHigh = () -> {
                updateGate.await();
                return putTemperatureRule(high, 10, 25);
            };
            Future<Integer> updateA = pool.submit(moveLow);
            Future<Integer> updateB = pool.submit(moveHigh);
            updateGate.countDown();
            List<Integer> updateStatuses = List.of(
                    updateA.get(20, TimeUnit.SECONDS), updateB.get(20, TimeUnit.SECONDS));
            assertEquals(1, updateStatuses.stream().filter(status -> status == 200).count());
            assertEquals(1, updateStatuses.stream().filter(status -> status == 409).count());

            JsonNode finalRules = jsonResponse("/api/configurations/temperature-rules");
            if (updateStatuses.getFirst() == 200) {
                assertRange(finalRules, low, 1, 18);
                assertRange(finalRules, high, 20, 25);
            } else {
                assertRange(finalRules, low, 1, 5);
                assertRange(finalRules, high, 10, 25);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void editedConfigurationChangesNextDeliveryFeeResponse() throws Exception {
        mockMvc.perform(put("/api/configurations/base-fees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(baseFeeJson("TALLINN", "CAR", "4.75")))
                .andExpect(status().isOk());

        OffsetDateTime at = OffsetDateTime.parse("2025-04-01T12:00:00Z");
        weatherRepository.saveAndFlush(new WeatherObservation("Tallinn-Harku", "fr05-fee-check",
                new BigDecimal("5"), new BigDecimal("0"), "Clear", at));
        mockMvc.perform(get("/api/delivery-fee")
                        .param("city", "TALLINN")
                        .param("vehicleType", "CAR")
                        .param("datetime", at.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.75));
    }

    private String baseFeeJson(String city, String vehicle, String amount) {
        return "{\"city\":\"" + city + "\",\"vehicleType\":\"" + vehicle
                + "\",\"feeAmount\":" + amount + "}";
    }

    private String temperatureJson(String vehicle, Integer lower, Integer upper,
            boolean lowerInclusive, boolean upperInclusive) {
        return "{\"vehicleType\":\"" + vehicle + "\",\"lowerThreshold\":"
                + (lower == null ? "null" : lower) + ",\"upperThreshold\":"
                + (upper == null ? "null" : upper) + ",\"lowerInclusive\":" + lowerInclusive
                + ",\"upperInclusive\":" + upperInclusive + ",\"feeAmount\":0.25}";
    }

    private String windJson(Integer lower, Integer upper) {
        return "{\"vehicleType\":\"SCOOTER\",\"lowerThreshold\":"
                + (lower == null ? "null" : lower) + ",\"upperThreshold\":"
                + (upper == null ? "null" : upper)
                + ",\"lowerInclusive\":true,\"upperInclusive\":true,"
                + "\"outcome\":\"SURCHARGE\",\"feeAmount\":0.25}";
    }

    private String phenomenonJson(String vehicle, String phenomenon, String amount) {
        return "{\"vehicleType\":\"" + vehicle + "\",\"phenomenon\":\"" + phenomenon
                + "\",\"outcome\":\"SURCHARGE\",\"feeAmount\":" + amount + "}";
    }

    private long jsonLong(MvcResult result, String field) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get(field).asLong();
    }

    private JsonNode jsonResponse(String path) throws Exception {
        String body = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private long phenomenonRuleId(String vehicle, String phenomenon) throws Exception {
        for (JsonNode rule : jsonResponse("/api/configurations/phenomenon-rules")) {
            if (rule.get("vehicleType").asString().equals(vehicle)
                    && rule.get("phenomenon").asString().equals(phenomenon)) {
                return rule.get("id").asLong();
            }
        }
        fail("No rule for " + vehicle + " and " + phenomenon);
        return -1;
    }

    private List<Long> idsFor(String path, String vehicle) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (JsonNode rule : jsonResponse(path)) {
            if (rule.get("vehicleType").asString().equals(vehicle)) {
                ids.add(rule.get("id").asLong());
            }
        }
        return ids;
    }

    private void deleteRulesForVehicle(String path, String vehicle) throws Exception {
        for (long id : idsFor(path, vehicle)) {
            mockMvc.perform(delete(path + "/{id}", id)).andExpect(status().isNoContent());
        }
    }

    private JsonNode onlyRuleForVehicle(JsonNode rules, String vehicle) {
        JsonNode result = null;
        int count = 0;
        for (JsonNode rule : rules) {
            if (rule.get("vehicleType").asString().equals(vehicle)) {
                result = rule;
                count++;
            }
        }
        assertEquals(1, count);
        return result;
    }

    private boolean hasBounds(JsonNode rule, int lower, int upper) {
        return rule.get("lowerThreshold").decimalValue().compareTo(BigDecimal.valueOf(lower)) == 0
                && rule.get("upperThreshold").decimalValue().compareTo(BigDecimal.valueOf(upper)) == 0;
    }

    private void assertRange(JsonNode rules, long id, int lower, int upper) {
        for (JsonNode rule : rules) {
            if (rule.get("id").asLong() == id) {
                assertEquals(lower, rule.get("lowerThreshold").intValue());
                assertEquals(upper, rule.get("upperThreshold").intValue());
                return;
            }
        }
        fail("Missing rule ID " + id);
    }

    private int postWindRule(int lower, int upper) throws Exception {
        return mockMvc.perform(post("/api/configurations/wind-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(windJson(lower, upper)))
                .andReturn().getResponse().getStatus();
    }

    private void postWindRuleExpecting(int lower, int upper, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, postWindRule(lower, upper));
    }

    private int putWindRule(long id, int lower, int upper) throws Exception {
        return mockMvc.perform(put("/api/configurations/wind-rules/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(windJson(lower, upper)))
                .andReturn().getResponse().getStatus();
    }

    private int postTemperatureRule(int lower, int upper) throws Exception {
        return mockMvc.perform(post("/api/configurations/temperature-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("SCOOTER", lower, upper, true, true)))
                .andReturn().getResponse().getStatus();
    }

    private void postTemperatureRuleExpecting(int lower, int upper, int expectedStatus)
            throws Exception {
        assertEquals(expectedStatus, postTemperatureRule(lower, upper));
    }

    private int putTemperatureRule(long id, int lower, int upper) throws Exception {
        return mockMvc.perform(put("/api/configurations/temperature-rules/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(temperatureJson("SCOOTER", lower, upper, true, true)))
                .andReturn().getResponse().getStatus();
    }
}
