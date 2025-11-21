# Stage 1: Build the application using a Gradle image
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .

# Execute the build, skipping tests for faster container builds
RUN gradle build --no-daemon -x test

# Stage 2: Create the final, smaller image using a JRE
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
# Copy the built JAR file from the 'build' stage
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
# Expose the port the application will run on (optional but good practice)
EXPOSE 8080 8081 8082
# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
