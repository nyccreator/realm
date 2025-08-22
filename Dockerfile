# Backend Dockerfile - Optimized for Neo4j connectivity
FROM openjdk:17-jdk-slim

# Neo4j-specific JVM optimizations
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:G1HeapRegionSize=16m"

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies in a separate layer for better caching
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Health check for Neo4j connectivity
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar target/realm-0.0.1-SNAPSHOT.jar"]