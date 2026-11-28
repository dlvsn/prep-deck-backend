# syntax=docker/dockerfile:1

# The backend as a container: built with Maven, run on a JRE as an unprivileged user.
# docker-compose.yaml next to this file starts it together with PostgreSQL and Mailpit.

# ---- build: dependencies first, so a change to the sources does not download them again
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/prep-deck-backend-*.jar app.jar

# ---- run
FROM eclipse-temurin:21-jre
WORKDIR /app
# Uploaded card pictures go to /data/images (IMAGES_STORAGE=LOCAL); compose mounts a volume there.
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --no-create-home app \
    && mkdir -p /data/images \
    && chown app:app /data/images
COPY --from=build /build/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
