# --- 1. Build the React/Vite frontend ----------------------------------------
FROM node:20-alpine AS frontend
WORKDIR /fe

# Install deps first so this layer is cached when only sources change.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# --- 2. Build the Spring Boot fat jar (frontend bundled as static assets) ----
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /src

COPY . .
# Bake the built frontend into Spring's static resources so Boot serves it at /.
COPY --from=frontend /fe/dist src/main/resources/static

# gradlew loses the +x bit when checked out on Windows; restore it.
RUN chmod +x gradlew \
 && ./gradlew --no-daemon bootJar -x test

# --- 3. Slim runtime image ---------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=backend /src/build/libs/*.jar app.jar

# Render injects its own PORT; default is fine for local docker runs too.
ENV PORT=8080
EXPOSE 8080

# MaxRAMPercentage scales the heap to the container memory limit — important
# on Render free (512 MB) where a fixed -Xmx is easy to misconfigure.
ENTRYPOINT ["sh","-c","java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT} -jar app.jar"]
