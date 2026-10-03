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

# ── Stage 2: dev runtime (docker-compose.yaml uses build.target: dev) ─────────
FROM eclipse-temurin:25-jre-alpine AS dev

WORKDIR /app

# curl is needed for the Docker healthcheck
RUN apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 3: prod runtime (docker-compose.prod.yaml uses build.target: prod) ──
FROM eclipse-temurin:25-jre-alpine AS prod

WORKDIR /app

# Same healthcheck semantics as dev — curl is a trivial add on an already-Alpine JRE base.
RUN apk add --no-cache curl \
    && addgroup -S polypilot && adduser -S polypilot -G polypilot

COPY --from=build /app/target/*.jar app.jar

RUN chown polypilot:polypilot app.jar
USER polypilot

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=120s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
