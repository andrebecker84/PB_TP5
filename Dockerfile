# Estágio 1: build do artefato
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Estágio 2: imagem de execução mínima
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/saikoo-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
