# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Reuse the BuildKit Maven cache so dependency resolution is not re-downloaded.
# -B/-ntp keep the build log small and non-interactive; the dependency layer is
# only invalidated when pom.xml changes.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q dependency:go-offline
COPY src ./src
# Tests are executed by `mvn verify` and CI, so skip compiling and running them here.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q package -Dmaven.test.skip=true

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
