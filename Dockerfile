FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.2_13_1.9.9_3.4.1 as build

WORKDIR /app
COPY . /app

RUN sbt assembly

FROM openjdk:21-jdk
WORKDIR /app

COPY --from=build /app/target/scala-*/*.jar /app/cron-resource.jar
COPY deploy/scripts/check /opt/resource/check
COPY deploy/scripts/in /opt/resource/in
COPY deploy/scripts/out /opt/resource/out