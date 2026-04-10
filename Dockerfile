# ABOUTME: Multi-stage Docker build for the Cyoda TMS.
# Two build targets: "backend" (default) and "frontend" (nginx).
# Generates a unique AES-256 encryption key per backend image build for OBO signing key storage.

# Stage 1: Build the frontend
FROM node:20-alpine AS frontend-builder

RUN corepack enable && corepack prepare pnpm@10.30.0 --activate

WORKDIR /app

COPY pnpm-lock.yaml pnpm-workspace.yaml package.json ./
COPY apps/frontend/package.json apps/frontend/

RUN pnpm install --frozen-lockfile --filter frontend

COPY apps/frontend/ apps/frontend/

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN pnpm --filter frontend build

# Stage 2: Build the backend
FROM gradle:8.5-jdk21 AS backend-builder

WORKDIR /app

COPY . .

RUN ./gradlew :apps:backend:bootJar

# Stage 3: Frontend image (nginx) — build with: docker build --target frontend
FROM nginx:1.27-alpine AS frontend

COPY --from=frontend-builder /app/apps/frontend/dist/ /usr/share/nginx/html/

EXPOSE 80

# Stage 4: Backend image (default target)
FROM eclipse-temurin:21-jdk AS backend

WORKDIR /app

COPY --from=backend-builder /app/apps/backend/build/libs/backend-1.0-SNAPSHOT.jar /app/app.jar

# Download OpenTelemetry Java agent for automatic instrumentation.
# The checksum is pinned to guard against a compromised release asset —
# bump both ARGs together whenever upgrading the agent.
ARG OTEL_AGENT_VERSION=2.11.0
ARG OTEL_AGENT_SHA256=4cff4ab46179260a61fc0d884f3f170cfbd9d2962dd260be2cff31262d0c7618
RUN curl -sL -o /app/opentelemetry-javaagent.jar \
      https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar \
    && echo "${OTEL_AGENT_SHA256}  /app/opentelemetry-javaagent.jar" | sha256sum -c -

# Generate a unique OBO encryption key for this image build.
RUN openssl rand -base64 32 > /app/obo-encryption-key.txt

EXPOSE 8080

ENV APP_CORS_ALLOWED_ORIGINS="*"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -javaagent:/app/opentelemetry-javaagent.jar"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=cloud -jar app.jar"]
