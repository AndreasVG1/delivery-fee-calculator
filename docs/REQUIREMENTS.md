# Requirements Specification

## 1. Overview

**Project:** Delivery Fee Calculator  
**Objective:** Develop a Spring Boot application that calculates food delivery fees based on region, vehicle type, and
weather conditions.

## 2. Functional Requirements

### FR-01: Weather Data Storage

**Description:** The application must store weather observations in an H2 database.

**Acceptance Criteria:**

- [ ] Weather observations contain all required fields.
- [ ] Weather observations can be persisted and retrieved.
- [ ] New imports do not overwrite historical records.

### FR-02: Weather Data Import

**Description:** The application must periodically import weather observations from an external API.

**Acceptance Criteria:**

- [ ] Weather data is retrieved from
  the [weather portal](https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php).
- [ ] Imported observations are persisted.
- [ ] CronJob is used for scheduling requests.
- [ ] The default request frequency is once every hour, 15 minutes after a full hour (HH:15:00)
- [ ] Import frequency is configurable.

### FR-03: Delivery Fee Calculation

**Description:** The application must calculate delivery fees according to defined business rules.

**Acceptance Criteria:**

- [ ] Regional base fees are applied correctly according to city and vehicle type.
- [ ] Air temperature based extra fees are applied correctly based on vehicle type and air temperature.
- [ ] Wind speed based extra fees are applied correctly based on vehicle type and wind speed.
- [ ] Weather phenomenon based extra fees are applied correctly based on vehicle type and phenomenon.
- [ ] Restricted delivery conditions are handled according to the specification.
- [ ] Regional base fees and weather-related extra fees can be managed through the REST interface.

### FR-04: REST API

**Description:** The application must expose an endpoint for requesting delivery fees.

**Acceptance Criteria:**

- [ ] Endpoint accepts the required input parameters (city and vehicle type).
- [ ] Endpoint accepts an optional datetime parameter.
- [ ] Valid requests return the calculated delivery fee based on the city, vehicle type and optional datetime.
- [ ] Invalid requests return appropriate error responses.

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

## 5. Open Questions and Assumptions

| ID   | Question / Assumption                                                                                          | Resolution                 |
|------|----------------------------------------------------------------------------------------------------------------|----------------------------|
| Q-01 | For datetime-specific requests, do we use current business rules, or also keep track of business rule changes? | Use currently active rules |

## 6. Requirement Traceability

| Requirement | Implementation | Test | Status      |
|-------------|----------------|------|-------------|
| FR-01       |                |      | Not started |
| FR-02       |                |      | Not started |
| FR-03       |                |      | Not started |
| FR-04       |                |      | Not started |

---

**Status values:** Not started / In progress / Completed
