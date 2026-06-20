# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first and download dependencies separately.
# Docker caches this layer — subsequent builds skip the download if pom.xml hasn't changed.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# curl is needed for the Docker healthcheck
RUN apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
