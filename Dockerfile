# Stage 1: build the jar with the same Gradle and JDK versions the project uses.
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Stage 2: run on a JRE only image, as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home trekops
COPY --from=build /app/build/libs/*.jar app.jar
USER trekops
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
