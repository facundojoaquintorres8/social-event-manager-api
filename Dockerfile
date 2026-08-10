# Etapa 1: build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copiar solo pom
COPY pom.xml .

RUN mvn dependency:go-offline

# 2. Copiar código
COPY src ./src

# 3. Build
RUN mvn clean package -DskipTests

# Etapa 2: run
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]