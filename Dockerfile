# ==============================================================================
# Multi-Stage Lean Dockerfile for Render Free Tier (512MB RAM Limit)
# ==============================================================================

# ----------------- Stage 1: Build JAR -----------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# ----------------- Stage 2: Ultra-Lean Runtime -----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the compiled JAR artifact and credentials from builder
COPY --from=builder /app/target/login-registration-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/credentials ./credentials

# Copy and set executable permissions on clean entrypoint wrapper
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]