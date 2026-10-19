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
