# services/merchant-service/Dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and root POM first (layer caching)
COPY mvnw mvnw.cmd ./
COPY .mvn/ .mvn/
COPY pom.xml ./

# Copy all module POMs to satisfy Maven reactor module definitions
COPY common/pom.xml common/
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/pricing-service/pom.xml services/pricing-service/
COPY services/parcel-service/pom.xml services/parcel-service/
COPY services/dispatch-service/pom.xml services/dispatch-service/
COPY services/merchant-service/pom.xml services/merchant-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY services/ai-assistant/pom.xml services/ai-assistant/

# Copy common module and the specific service module
COPY common/ common/
COPY services/merchant-service/ services/merchant-service/

# Build only this module and its deps (common), skip tests
RUN chmod +x mvnw && ./mvnw -pl services/merchant-service -am package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user (conventions.md §7)
RUN addgroup -S swiftdrop && adduser -S swiftdrop -G swiftdrop
USER swiftdrop

# Copy the fat JAR from build stage
COPY --from=builder /build/services/merchant-service/target/*.jar app.jar

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
