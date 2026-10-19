FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN mkdir -p /data \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app \
    && chown app:app /data

COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
COPY --chown=app:app docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh

VOLUME ["/data"]
EXPOSE 8080
USER app

ENTRYPOINT ["/app/docker-entrypoint.sh"]
