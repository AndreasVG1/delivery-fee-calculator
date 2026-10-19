# Requirements Specification

## 1. Overview

**Project:** Delivery Fee Calculator  
**Objective:** Develop a Spring Boot application that calculates food delivery fees based on region, vehicle type, and
weather conditions.

## 2. Functional Requirements

### FR-01: Weather Data Storage

**Description:** The application must store weather observations in an H2 database.

**Acceptance Criteria:**

- [x] Weather observations contain all required fields.
- [x] The imported weather phenomenon is retained as the exact API string; any mapping to fee-rule values is performed separately.
- [x] Weather observations can be persisted and retrieved.
- [x] New imports do not overwrite historical records.

### FR-02: Weather Data Import

**Description:** The application must periodically import weather observations from an external API.

**Acceptance Criteria:**

- [x] Weather data is retrieved from
  the [weather portal](https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php).
- [x] Imported observations are persisted.
- [x] CronJob is used for scheduling requests.
- [x] The default request frequency is once every hour, 15 minutes after a full hour (HH:15:00)
- [x] Import frequency is configurable.

### FR-03: Delivery Fee Calculation

**Description:** The application must calculate delivery fees according to defined business rules.

**Acceptance Criteria:**

- [x] Regional base fees are applied correctly according to city and vehicle type.
- [x] Air temperature based extra fees are applied correctly based on vehicle type and air temperature.
- [x] Wind speed based extra fees are applied correctly based on vehicle type and wind speed.
- [x] Weather phenomenon based extra fees are applied correctly based on vehicle type and phenomenon.
- [x] Restricted delivery conditions are handled according to the specification.

### FR-04: REST API

**Description:** The application must expose an endpoint for requesting delivery fees.

**Acceptance Criteria:**

- [x] Endpoint accepts the required input parameters (city and vehicle type).
- [x] Endpoint accepts an optional datetime parameter.
- [x] Valid requests return the calculated delivery fee based on the city, vehicle type and optional datetime.
- [x] Invalid requests return appropriate error responses.

### FR-05: Business rules REST interface

**Description:** The regional base fees and weather-related extra fees must configurable through a REST interface.

**Acceptance Criteria:**

- [ ] Regional base fees and weather-related extra fees can be managed through the REST interface.
- [ ] Creating or updating a temperature or wind rule is rejected if its range overlaps another rule of the same type and vehicle type. Tests cover unbounded ranges, equal endpoints with each inclusivity combination, and adjacent non-overlapping ranges.
- [ ] Concurrent creates or updates of overlapping rules cannot both succeed; after the requests finish, the stored ranges for each rule type and vehicle type remain non-overlapping.

### FR-06: Delivery Fee Calculation Storage

**Description:** The application must store delivery fee calculations so recorded results can be displayed after fee rules change.

**Acceptance Criteria:**

- [ ] Each calculation stores its request details, calculation datetime, the selected weather observation when available, and its component fees and total when successful.
- [ ] Failed calculations can be stored with an error message and no total fee.
- [ ] Retrieving a stored calculation returns the recorded values without recalculating them using current fee rules.
- [ ] A successful stored calculation is eligible for reuse only when its city and vehicle type match and its referenced observation's `observed_at` is within 24 hours of the requested datetime.

## 3. Non-Functional Requirements

| ID     | Requirement                          | Verification                       |
|--------|--------------------------------------|------------------------------------|
| NFR-01 | Use Java and Spring                  | Dependency/build inspection        |
| NFR-02 | Use H2 database                      | Configuration and integration test |
| NFR-03 | Follow OOP and clean code principles | Code review                        |
| NFR-04 | Apply appropriate layering           | Architecture review                |
| NFR-05 | Document public methods              | JavaDoc inspection                 |
| NFR-06 | Implement error handling             | Error scenario tests               |
| NFR-07 | Provide automated tests              | Test execution                     |
| NFR-08 | Organize code in version control     | Git repository review              |
| NFR-09 | Containerize code for distribution   | Docker build succeeds              |

## 4. Business Rules

These are the default rules. Rules can be managed from a separate REST interface. 

### 4.1 Delivery Fee Formula

**BR-01:** The total delivery fee is calculated as:

**Delivery Fee = RBF + ATEF + WSEF + WPEF**

Where:

- **RBF:** Regional Base Fee
- **ATEF:** Air Temperature Extra Fee
- **WSEF:** Wind Speed Extra Fee
- **WPEF:** Weather Phenomenon Extra Fee

If a vehicle restriction applies, the calculation must return an error instead of a fee.

### 4.2 Regional Base Fee (RBF)

**BR-02:** The regional base fee depends on the city and vehicle type.

| City    | Car    | Scooter | Bike   |
|---------|--------|---------|--------|
| Tallinn | 4.00 € | 3.50 €  | 3.00 € |
| Tartu   | 3.50 € | 3.00 €  | 2.50 € |
| Pärnu   | 3.00 € | 2.50 €  | 2.00 € |

### 4.3 Air Temperature Extra Fee (ATEF)

**BR-03:** A temperature surcharge applies only to Scooter and Bike.

| Air Temperature | Car | Scooter | Bike   |
|-----------------|-----|---------|--------|
| T < -10°C       | 0 € | 1.00 €  | 1.00 € |
| -10°C ≤ T < 0°C | 0 € | 0.50 €  | 0.50 € |
| T ≥ 0°C         | 0 € | 0 €     | 0 €    |

### 4.4 Wind Speed Extra Fee (WSEF)

**BR-04:** A wind speed surcharge or restriction applies only to Bike.

| Wind Speed      | Car | Scooter | Bike      |
|-----------------|-----|---------|-----------|
| W < 10 m/s      | 0 € | 0 €     | 0 €       |
| 10 ≤ W ≤ 20 m/s | 0 € | 0 €     | 0.50 €    |
| W > 20 m/s      | 0 € | 0 €     | Forbidden |

### 4.5 Weather Phenomenon Extra Fee (WPEF)

**BR-05:** A weather phenomenon surcharge or restriction applies only to Scooter and Bike.

| Weather Phenomenon      | Car | Scooter   | Bike      |
|-------------------------|-----|-----------|-----------|
| Snow or sleet           | 0 € | 1.00 €    | 1.00 €    |
| Rain                    | 0 € | 0.50 €    | 0.50 €    |
| Glaze, hail, or thunder | 0 € | Forbidden | Forbidden |
| Other phenomena         | 0 € | 0 €       | 0 €       |

### 4.6 Vehicle Restrictions

**BR-06:** If any weather condition prohibits the selected vehicle type, the delivery fee must not be returned.

**Error message:**
`Usage of selected vehicle type is forbidden`

Restrictions:

- Bike: Wind speed greater than 20 m/s.
- Scooter and Bike: Glaze, hail, or thunder.

### 4.7 Threshold Rule Ranges

**BR-07:** Temperature and wind fee rules may have a nullable `lower_threshold`, a nullable `upper_threshold`, or both. At least one threshold must be present. A null lower threshold means the range has no lower bound; a null upper threshold means it has no upper bound. Inclusivity flags determine whether a value equal to a present threshold is included.

For example, a rule for air temperature less than -10°C has `lower_threshold = NULL`, `upper_threshold = -10.0`, and an exclusive upper bound.

**BR-08:** Threshold ranges for the same rule type and vehicle type must not overlap. Creating a rule whose range overlaps an existing rule's range must be rejected.

**BR-09:** If a measured temperature or wind speed falls into no configured rule range during fee calculation, calculation must return an error instead of a fee.

**Error message:**
`Uncovered range`

### 4.8 Regional Base Fee Uniqueness

**BR-10:** Only one regional base fee may exist for a given city and vehicle type. Creating a regional base fee when one already exists for that pair must be rejected.

### 4.9 Weather Observation Selection

**BR-11:** The weather station for each city is:

| City    | Weather station |
|---------|-----------------|
| Tallinn | Tallinn-Harku   |
| Tartu   | Tartu-Tõravere  |
| Pärnu   | Pärnu           |

For the requested datetime, the calculator must use the observation from the mapped station whose `observed_at` timestamp is nearest to the requested datetime, provided the absolute difference is no more than 24 hours. If no observation falls within that window, calculation must return an error instead of a fee.

**Error message:**
`No weather data available for the requested time`

### 4.10 Delivery Fee Calculation

**BR-12:** Each delivery fee calculation must be stored as a `DeliveryFeeCalculation` containing its ID, requested datetime, calculation datetime, city, vehicle type, a reference to the selected weather observation when one is available, regional base fee (`rbf`), temperature extra fee (`atef`), wind speed extra fee (`wsef`), weather phenomenon extra fee (`wpef`), and total fee when successful. The entity may contain an `error_message` when calculation fails; in that case, no total fee is returned.

**BR-13:** For a request with a datetime, the application must first look for a successful `DeliveryFeeCalculation` whose city and vehicle type match the request and whose referenced weather observation's `observed_at` is within 24 hours of the requested datetime. If multiple calculations match, the one referencing the nearest observation must be used. The stored component amounts and total are returned without recalculation using current rules.

If no successful calculation matches, the application must use the nearest weather observation from the station mapped to the requested city, provided its `observed_at` is within 24 hours of the requested datetime, and calculate using currently active fee rules. If no matching observation is available, calculation must return an error.

**Error message:**
`No weather data available for the requested time`

## 5. Open Questions and Assumptions

| ID   | Question / Assumption                                                                                          | Resolution                 |
|------|----------------------------------------------------------------------------------------------------------------|----------------------------|
| Q-01 | For datetime-specific requests, do we use current business rules, or also keep track of business rule changes? | Use currently active rules |

## 6. Requirement Traceability

| Requirement | Implementation                                             | Test                                                                                                            | Status      |
|-------------|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|-------------|
| FR-01       | `WeatherObservation`, `WeatherRepository`                  | `WeatherRepositoryTest` (H2)                                                                                    | Completed   |
| FR-02       | `WeatherClient`, `WeatherImportService`, `ImportScheduler` | `WeatherClientTest`, `WeatherImportServiceTest` (H2), `ImportSchedulerTest`, `ImportSchedulerConfigurationTest` | Completed   |
| FR-03       | `FeeConfigurationService`, `FeeCalculator`, `DeliveryFeeService` | `FeeCalculatorTest`, `DeliveryFeeServiceTest`                                                   | Completed   |
| FR-04       | `DeliveryFeeController`                                    | `DeliveryFeeControllerTest` (MockMvc, Spring context, H2)                                                       | Completed   |
| FR-05       |                                                            |                                                                                                                 | Not started |
| FR-06       |                                                            |                                                                                                                 | Not started |

---

**Status values:** Not started / In progress / Completed
