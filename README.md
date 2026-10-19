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
