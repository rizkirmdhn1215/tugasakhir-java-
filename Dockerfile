FROM maven:3.9.6-openjdk-17-slim AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/skripsi-0.0.1-SNAPSHOT.jar app.jar

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Run the application
EXPOSE 8080
CMD ["java", "-Dspring.profiles.active=railway", "-jar", "app.jar"]

## Alternative Solution (More Robust)

If you continue having issues, use Maven directly instead of the wrapper: