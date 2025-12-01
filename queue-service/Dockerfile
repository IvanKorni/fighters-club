FROM gradle:8.14.2-jdk24 AS build

WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY openapi ./openapi

RUN gradle --no-daemon clean build -x test publishToMavenLocal

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8093
CMD ["java", "-jar", "app.jar"]
