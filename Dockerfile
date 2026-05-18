# Stage 1: Build the Angular frontend
FROM node:20-alpine AS node-builder
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Spring Boot backend
FROM maven:3.9-eclipse-temurin-21 AS java-builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
# Copy the compiled Angular frontend assets to the static resources of Spring Boot
COPY --from=node-builder /app/dist/frontend/browser/ ./src/main/resources/static/
RUN mvn clean package -DskipTests

# Stage 3: Lightweight production runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=java-builder /app/target/flowguard-server-1.0.0-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
