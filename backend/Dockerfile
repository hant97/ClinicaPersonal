# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src src
# Utilizamos el maven nativo de la imagen para evitar problemas de permisos con el wrapper en linux
RUN mvn -B clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# La aplicación no necesita privilegios de root.
RUN addgroup -S clinica && adduser -S -G clinica clinica \
    && mkdir -p /app/uploads && chown clinica:clinica /app/uploads
COPY --from=build --chown=clinica:clinica /app/target/*.jar app.jar
USER clinica

# La JVM dimensiona el heap según el límite de memoria del contenedor (cgroup), no el del host.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    STORAGE_LOCAL_ROOT=/app/uploads

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health/liveness" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
