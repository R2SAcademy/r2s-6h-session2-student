# ==============================================================================
# R2S MASTERCLASS - SESSION 2: DOCKERIZE PRODUCTION LAB TASK
# ==============================================================================
# TODO [Lab Task 4]: Write an Enterprise Multi-Stage Dockerfile!
#
# Constraints & Requirements:
# 1. Multi-Stage Build:
#    - Stage 1 (Builder): Use `maven:3.9.6-eclipse-temurin-17-alpine` to compile & package.
#      Leverage Docker layer caching by copying `pom.xml` and running `mvn dependency:go-offline`.
#      Extract spring boot layertools via:
#      `RUN java -Djarmode=layertools -jar target/*.jar extract`
#    - Stage 2 (Runtime): Use `eclipse-temurin:17-jre-alpine`.
#      Copy extracted layers from builder: dependencies, spring-boot-loader, snapshot-dependencies, application.
# 2. Security (DevSecOps):
#    - Run as a non-root user (e.g. `addgroup -S appgroup && adduser -S appuser -G appgroup`, `USER appuser`).
# 3. Performance & Footprint:
#    - Target Image Size: < 120MB (Standard JDK images are >800MB!).
# 4. Reliability:
#    - Add Container HEALTHCHECK probe on http://localhost:8080/actuator/health.
# ==============================================================================

# Write your Dockerfile implementation below:
