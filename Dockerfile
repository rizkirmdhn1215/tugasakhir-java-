FROM openjdk:17-jdk-slim

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src src
RUN mvn clean package -DskipTests

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Run the application
EXPOSE 8080
CMD ["java", "-Dspring.profiles.active=railway", "-jar", "target/skripsi-0.0.1-SNAPSHOT.jar"]