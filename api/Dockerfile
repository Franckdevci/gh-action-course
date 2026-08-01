# syntax=docker/dockerfile:1.7

# ---------- Stage 1 : build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -e -DskipTests package \
 && cp target/ecotrack-api-*.jar target/app.jar

# ---------- Stage 2 : runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
 && addgroup -S spring \
 && adduser  -S -G spring -h /app spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/app.jar /app/app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
