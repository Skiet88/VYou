# syntax=docker/dockerfile:1

# ---- Frontend: produces frontend/dist, which the backend build embeds as static resources ----
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- Backend: one fat jar, with the built frontend bundled in (see pom.xml's <resources>) ----
FROM eclipse-temurin:17-jdk-alpine AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src/ src/
COPY --from=frontend /app/frontend/dist/ frontend/dist/
RUN ./mvnw -B -DskipTests package

# ---- Runtime: slim JRE, no build tools ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S vyou2 && adduser -S vyou2 -G vyou2
COPY --from=backend /app/target/vyou2-*.jar app.jar
USER vyou2
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
