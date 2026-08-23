#!/bin/sh
set -e

# Strip any conflicting JVM options passed by host/platform environment
unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS
unset JAVA_OPTS

echo "Starting Spring Boot on Render with ultra-lean memory JVM profile..."

# Execute Java with strict single Serial GC and memory bounds (Ideal for Render 512MB limit)
exec java \
  -Xmx256m \
  -Xms64m \
  -Xss512k \
  -XX:MaxMetaspaceSize=160m \
  -XX:CompressedClassSpaceSize=32m \
  -XX:ReservedCodeCacheSize=48m \
  -XX:+UseSerialGC \
  -XX:TieredStopAtLevel=1 \
  -XX:CICompilerCount=2 \
  -Dspring.main.lazy-initialization=true \
  -Djava.security.egd=file:/dev/./urandom \
  -Dserver.port=${PORT:-8080} \
  -jar app.jar
