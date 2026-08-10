# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw.cmd pom.xml ./

COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app

COPY --from=build --chown=appuser:appuser /workspace/target/recommendation-platform-*.jar app.jar

USER appuser:appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
