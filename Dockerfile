FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# ✅ Install Maven manually
RUN apk add --no-cache maven

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Final lightweight image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/reporter-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]