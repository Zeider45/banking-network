# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build - Compila el proyecto con Maven
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiamos el pom.xml primero para aprovechar la caché de capas Docker:
# si el pom.xml no cambia, Maven no descargará dependencias en cada build
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime - Imagen mínima solo con JRE para producción
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario sin privilegios de root para seguridad
RUN addgroup -S banking && adduser -S banking -G banking
USER banking

# Copiamos solo el jar construido en el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Puerto por defecto (sobreescrito por SERVER_PORT en docker-compose)
EXPOSE 8080

# Opciones de JVM: limita memoria y habilita telemetría de contenedor
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
