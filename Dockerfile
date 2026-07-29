# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY recoflow/.mvn .mvn
COPY recoflow/mvnw recoflow/mvnw.cmd recoflow/pom.xml ./

COPY recoflow/src src

RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S recoflow && adduser -S recoflow -G recoflow

WORKDIR /app

COPY --from=build --chown=recoflow:recoflow /workspace/target/recoflow-*.jar app.jar

USER recoflow:recoflow

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
