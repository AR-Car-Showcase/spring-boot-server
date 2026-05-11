# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/AR-car-showcase-server-0.0.1-SNAPSHOT.jar app.jar

# Create directory for models if not existing
RUN mkdir -p src/main/resources/static/models

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
