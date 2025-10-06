# Multi-stage build for optimal image size
# Build stage - sử dụng Gradle với Amazon Corretto
FROM gradle:8.11-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN ./gradlew bootJar --no-daemon

# ---- Run Stage ----
# Sử dụng Amazon Corretto JDK 21 - Optimized JDK by AWS
FROM amazoncorretto:21.0.5-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
