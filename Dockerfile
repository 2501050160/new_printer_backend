# ==============================================================================
# Multi-Stage Lean Dockerfile for Render Free Tier (512MB RAM Limit)
# ==============================================================================

# ----------------- Stage 1: Build JAR -----------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY . .

RUN chmod +x mvnw
# Build with minimal memory allocation during packaging
RUN MAVEN_OPTS="-Xmx256m -XX:+UseSerialGC" ./mvnw clean package -DskipTests

# ----------------- Stage 2: Ultra-Lean Runtime -----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the compiled JAR artifact from builder
COPY --from=builder /app/target/login-registration-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/credentials ./credentials

EXPOSE 8080

# Environment variables for low-memory container execution
ENV JAVA_OPTS="-Xmx256m -Xms128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -Djava.security.egd=file:/dev/./urandom"

# Launch Spring Boot with strict memory caps (Keeps memory at ~200MB-260MB, well below Render 512MB limit)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]