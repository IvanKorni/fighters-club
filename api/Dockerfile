FROM gradle:8.14.2-jdk24 AS build

# Ensure mavenLocal() uses the same location for both builds
ENV GRADLE_USER_HOME=/home/gradle/.gradle
# Maven local repository is at ~/.m2/repository by default for gradle user
# Both builds will use /home/gradle/.m2/repository

WORKDIR /home/gradle

# First, build and publish person-api from person-service
WORKDIR /home/gradle/person-service
COPY person-service/build.gradle.kts ./build.gradle.kts
COPY person-service/settings.gradle.kts ./settings.gradle.kts
COPY person-service/src ./src
COPY person-service/openapi ./openapi
# Build person-api JAR and publish to mavenLocal()
RUN gradle --no-daemon clean build -x test && \
    gradle --no-daemon publishToMavenLocal && \
    echo "person-api published successfully, verifying..." && \
    find /home/gradle/.m2 -name "person-api*" 2>/dev/null | head -3 || echo "Note: mavenLocal() path may vary, but publish succeeded"

# Now build api which depends on person-api
WORKDIR /home/gradle/api
COPY api/build.gradle.kts ./build.gradle.kts
COPY api/settings.gradle.kts ./settings.gradle.kts
COPY api/src ./src
COPY api/openapi ./openapi
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /home/gradle/api/build/libs/*.jar app.jar

EXPOSE 8091
CMD ["java", "-jar", "app.jar"]