# Delivery Fee Calculator

## Run with Docker

Build the application image and start the service:

```sh
cp .env.example .env
docker compose up --build
```

The application is available at <http://localhost:8080>. Compose stores the
file-backed H2 database in the `h2-data` named volume mounted at `/data`.
On the first start, Spring Boot loads `schema.sql` and `data.sql` from the
application resources. Later starts use the existing database without
reapplying the scripts. The database console is available at <http://localhost:8080/h2-console>

Stop the application with `docker compose down`; the database volume is kept.
To remove the database as well, run `docker compose down --volumes`.

## Delivery fee API

Request a delivery fee with `GET /api/delivery-fee`. The `city` and
`vehicleType` query parameters are required; `datetime` is optional and must
be an ISO 8601 date-time with an offset. If omitted, the service uses current
conditions.

```sh
curl --get 'http://localhost:8080/api/delivery-fee' \
  --data-urlencode 'city=Tartu' \
  --data-urlencode 'vehicleType=Scooter' \
  --data-urlencode 'datetime=2025-01-15T12:00:00Z'
```

A successful response contains the canonical city and vehicle names, the
requested datetime (or `null` when omitted), and the numeric total fee:

```json
{"city":"TARTU","vehicleType":"SCOOTER","requestedAt":"2025-01-15T12:00:00Z","totalFee":5.00}
```

Errors use `{"error":"..."}`: malformed or missing parameters return `400`,
missing weather data returns `404`, and forbidden vehicles or uncovered fee
rules return `422`.

## Fee configuration API

Manage active regional base fees and weather fee rules under
`/api/configurations`. Each collection supports `GET` to list and `POST` to
create; each resource supports `GET`, `PUT`, and `DELETE` by numeric ID:

- `/api/configurations/base-fees`
- `/api/configurations/temperature-rules`
- `/api/configurations/wind-rules`
- `/api/configurations/phenomenon-rules`

`PUT` replaces every field. JSON uses the entity field names and canonical enum
values (`TALLINN`, `CAR`, `SURCHARGE`, and so on). Range rules require at least
one threshold and cannot overlap another rule of the same type and vehicle;
null thresholds are unbounded. Base fees are unique by city and vehicle, and
phenomenon rules are unique by phenomenon and vehicle. Create returns `201`
with a `Location` header, reads and updates return `200`, and deletion returns
`204`. Invalid data returns `400`, missing IDs return `404`, and duplicate or
overlapping rules return `409`; errors use `{"error":"..."}`.

For example, list the active temperature rules:

```sh
curl -i http://localhost:8080/api/configurations/temperature-rules
```

## Weather data import

The application imports observations for Tallinn-Harku, Tartu-Tõravere, and
Pärnu every hour at HH:15 in the `Europe/Tallinn` time zone. Set
`weather.import.cron` in `application.properties` or the
`WEATHER_IMPORT_CRON` environment variable to change the schedule; the new
value takes effect after restarting the application. The default is
`0 15 * * * *`.

The portal endpoint can be changed with `weather.import.url` (or
`WEATHER_IMPORT_URL`). HTTP connection and request timeouts are configurable
with `weather.import.connect-timeout` and `weather.import.request-timeout`.

Weather observations come from the [Estonian Environment Agency weather
portal](https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php). Please
credit the Estonian Environment Agency and link to
[ilmateenistus.ee](https://www.ilmateenistus.ee/) when using these data.
