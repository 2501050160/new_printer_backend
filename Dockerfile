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

# Copy the compiled JAR artifact from builder
COPY --from=builder /app/target/login-registration-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/credentials ./credentials

EXPOSE 8080

# Conflict-free low-memory JVM options (Avoids multiple GC collision with JAVA_TOOL_OPTIONS)
ENV JAVA_OPTS="-Xmx256m -Xms128m -Xss512k -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]