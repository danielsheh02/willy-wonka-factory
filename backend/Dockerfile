FROM gradle:8.13-jdk17 AS build
WORKDIR /app

COPY . /app
RUN gradle bootJar --no-daemon

FROM openjdk:17.0-jdk
WORKDIR /app

COPY --from=build /app/build/libs/app.jar ./app.jar

EXPOSE 7999
CMD ["java", "-jar", "app.jar"]