# ─── STAGE 1: Build Image ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml and checkstyle.xml first to leverage Docker layer caching
COPY pom.xml checkstyle.xml ./

# Copy Source Code and compile production executable artifact
COPY src/ src/
RUN mvn clean package -DskipTests -B

# ─── STAGE 2: Production Runtime Image ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

# OpenContainers Initiative (OCI) Standard Image Metadata Labels
LABEL org.opencontainers.image.title="vetra-backend" \
      org.opencontainers.image.description="Vetra Livestock & Veterinary Healthcare Core Backend Engine" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Vetra Healthcare Technology" \
      org.opencontainers.image.source="https://github.com/omrajput14/vetra-backend" \
      org.opencontainers.image.licenses="Proprietary"

# Install wget for lightweight container healthcheck
RUN apk add --no-cache wget

# Create non-root system user and group for container security compliance
RUN addgroup -S vetra && adduser -S vetra -G vetra

WORKDIR /app

# Copy compiled JAR artifact from builder stage
COPY --from=builder /build/target/vetra-backend-*.jar /app/vetra-backend.jar

# Enforce file permissions for non-root execution
RUN chown -R vetra:vetra /app

USER vetra

# Expose HTTP web server application port
EXPOSE 8080

# Environment variables defaults for container runtime
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE="prod"

# Container Healthcheck using Spring Boot Actuator Liveness probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Launch Spring Boot Application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/vetra-backend.jar"]
