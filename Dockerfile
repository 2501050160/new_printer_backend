
# ==============================================================================
# Multi-Stage Lean Dockerfile for Render Free Tier (512MB RAM Limit)
# ==============================================================================

# ----------------- Stage 1: Build JAR -----------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -Dmaven.test.skip=true

# ----------------- Stage 2: Ultra-Lean Runtime -----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the compiled JAR artifact and credentials from builder
COPY --from=builder /app/target/login-registration-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/credentials ./credentials

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "unset JAVA_TOOL_OPTIONS; unset _JAVA_OPTIONS; unset JAVA_OPTS; exec java -Xmx256m -Xms64m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:CompressedClassSpaceSize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Dspring.main.lazy-initialization=true -Djava.security.egd=file:/dev/./urandom -jar app.jar"]