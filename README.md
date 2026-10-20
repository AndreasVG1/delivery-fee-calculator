# Delivery Fee Calculator

A Spring Boot REST service that calculates food delivery fees for Tallinn, Tartu, and Pärnu. It combines a city and vehicle base fee with surcharges or restrictions from weather observations imported from the Estonian Environment Agency. Fee rules can be managed through an API, and calculation results are stored for later retrieval.

## Test

Java 21 or newer is required for local development. The repository includes the Maven wrapper, so a separate Maven installation is unnecessary.

```sh
./mvnw test
```

On Windows, use `mvnw.cmd test`. The tests use H2 and do not require Docker or a running weather service.

## Run

### Locally

```sh
./mvnw spring-boot:run
```

The API listens on <http://localhost:8080>. With the default settings, local runs use an in-memory H2 database. The schema and default fee rules are loaded on startup; the data is lost when the process stops.

### With Docker Compose

```sh
cp .env.example .env
docker compose up --build
```

Compose exposes the API at <http://localhost:8080> and stores its H2 database in the `h2-data` volume. The schema and default rules are loaded when the database is first created. Later starts retain both imported weather and changes to fee rules.

```sh
docker compose down            # Stop; keep the database
docker compose down --volumes  # Stop and remove the database
```

The H2 console is at <http://localhost:8080/h2-console>. For the Compose setup, use the JDBC URL `jdbc:h2:file:/data/fee-calculator`, user and password defined in .env. The console is intended for development.

Weather observations are imported for Tallinn-Harku, Tartu-Tõravere, and Pärnu every hour at **HH:15 Europe/Tallinn**. A new database may return `404` for fee requests until its first import. Set `WEATHER_IMPORT_CRON` to change the schedule (default `0 15 * * * *`) or `WEATHER_IMPORT_URL` to change the source; restart the service after changing either value. Connection and request timeouts are configurable with `weather.import.connect-timeout` and `weather.import.request-timeout`.

## REST API

The examples below assume the service is running at `http://localhost:8080`. Monetary values are in euros. Supported cities are `TALLINN`, `TARTU`, and `PÄRNU`; vehicle types are `CAR`, `SCOOTER`, and `BIKE`.

### Calculate a delivery fee

`GET /api/delivery-fee` requires `city` and `vehicleType`. Their values are case insensitive. The optional `datetime` must be an ISO 8601 date-time with an offset, such as `2026-10-20T12:00:00+03:00`. Without it, the service calculates for the current time.

```sh
curl --get 'http://localhost:8080/api/delivery-fee' \
  --data-urlencode 'city=Tartu' \
  --data-urlencode 'vehicleType=Scooter'
```

An example successful response (the ID and fee depend on the stored weather and active rules):

```json
{"id":42,"city":"TARTU","vehicleType":"SCOOTER","requestedAt":null,"totalFee":4.50}
```

To query a particular time:

```sh
curl --get 'http://localhost:8080/api/delivery-fee' \
  --data-urlencode 'city=Pärnu' \
  --data-urlencode 'vehicleType=BIKE' \
  --data-urlencode 'datetime=2026-10-20T12:00:00+03:00'
```

The service uses an observation from the city's station within 24 hours of the requested time. A request with `datetime` first reuses a successful stored calculation for the same city and vehicle when its observation falls within that window; otherwise it calculates with the active rules. Requests without `datetime` create a new record.

Every new calculation with valid parameters is recorded, including business failures. `400` means invalid parameters (no record is created), `404` means no nearby weather observation, and `422` means a forbidden vehicle or missing rule coverage. Recorded failures include an `id` alongside `error`.

### Retrieve a recorded calculation

Use the ID returned by the calculation endpoint:

```sh
curl 'http://localhost:8080/api/delivery-fee/42'
```

The response includes `requestedAt`, `calculatedAt`, `city`, `vehicleType`, `weatherObservationId`, the four fee components (`rbf`, `atef`, `wsef`, `wpef`), `totalFee`, and `errorMessage`. It returns the stored result without recalculating it. An unknown ID returns `404`.

### Query and manage fee rules

The following collections each support `GET` (list) and `POST` (create). Append `/{id}` for `GET` (read), `PUT` (replace), and `DELETE` (remove):

| Collection | Fields for `POST` and `PUT` |
| --- | --- |
| `/api/configurations/base-fees` | `city`, `vehicleType`, `feeAmount` |
| `/api/configurations/temperature-rules` | `vehicleType`, `lowerThreshold`, `upperThreshold`, `lowerInclusive`, `upperInclusive`, `feeAmount` |
| `/api/configurations/wind-rules` | Temperature rule fields, plus `outcome` |
| `/api/configurations/phenomenon-rules` | `vehicleType`, `phenomenon`, `outcome`, `feeAmount` |

For example, list base fees and inspect one rule:

```sh
curl 'http://localhost:8080/api/configurations/base-fees'
curl 'http://localhost:8080/api/configurations/temperature-rules/1'
```

To replace a base fee, first find its ID in the list response, then send **all** fields. This example changes the seeded Tallinn car fee (ID `1` on a fresh database):

```sh
curl -X PUT 'http://localhost:8080/api/configurations/base-fees/1' \
  -H 'Content-Type: application/json' \
  -d '{"city":"TALLINN","vehicleType":"CAR","feeAmount":4.75}'
```

For range rules, at least one threshold must be non-null. A null lower or upper threshold leaves that side unbounded; the inclusivity flags are required. Ranges for the same vehicle and rule type cannot overlap. `outcome` is `SURCHARGE` or `FORBIDDEN`; forbidden rules have a zero fee. Phenomenon values are `SNOW`, `SLEET`, `RAIN`, `GLAZE`, `HAIL`, `THUNDER`, and `OTHER`. Configuration writes return `201` for creation, `200` for replacement, and `204` for deletion. Invalid data returns `400`, unknown IDs return `404`, and duplicate or overlapping rules return `409`, with an `{"error":"..."}` body.

## Weather data credit

Weather observations come from the [Estonian Environment Agency weather portal](https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php). Credit the Estonian Environment Agency and link to [ilmateenistus.ee](https://www.ilmateenistus.ee/) when using these data.
