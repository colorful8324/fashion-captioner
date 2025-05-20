# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the entire source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install curl and jq
RUN apt-get update && \
    apt-get install -y curl jq && \
    rm -rf /var/lib/apt/lists/*

# Copy the jar from the builder stage
COPY --from=builder /app/target/fashion-captioner-0.0.1-SNAPSHOT.jar /app/fashion-captioner.jar

# Copy the entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose the Spring Boot port
EXPOSE 8081

# Run the application using the entrypoint script
ENTRYPOINT ["/entrypoint.sh"]
