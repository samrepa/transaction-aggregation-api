# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven settings first (resolves from Maven Central)
COPY settings.xml .

# Cache dependencies — only re-downloads when pom.xml changes
COPY pom.xml .
RUN mvn -s settings.xml dependency:go-offline -q

# Copy source and build (tests run separately — see README)
COPY src ./src
RUN mvn -s settings.xml package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/target/transaction-aggregation-api-*.jar app.jar

# Expose application port
EXPOSE 8080

# JVM tuning for containers:
#   UseContainerSupport  → respects cgroup memory limits
#   MaxRAMPercentage     → use up to 75% of container RAM for heap
#   ExitOnOutOfMemoryError → fail fast instead of silently degrading
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
