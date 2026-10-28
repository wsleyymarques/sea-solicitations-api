FROM maven:3.10.0-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine-3.24 AS runtime

WORKDIR /app

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
