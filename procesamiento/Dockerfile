# syntax=docker/dockerfile:1

# ---------- Etapa de build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias: se resuelven una sola vez mientras el pom no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compilacion y empaquetado del jar ejecutable
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Etapa de runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/target/*.jar /app/app.jar

# Usuario sin privilegios
RUN groupadd --system spring \
    && useradd --system --gid spring spring \
    && chown -R spring:spring /app
USER spring:spring

EXPOSE 5003
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
