# ─── STAGE 1: Build Image ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ─── STAGE 2: Production Runtime Image ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

RUN apk add --no-cache wget
RUN addgroup -S vetra && adduser -S vetra -G vetra

WORKDIR /app

COPY --from=builder /build/target/vetra-backend-*.jar /app/vetra-backend.jar
RUN chown -R vetra:vetra /app

USER vetra
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/vetra-backend.jar"]
