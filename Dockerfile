FROM eclipse-temurin:21.0.1_12-jdk-alpine  as build

RUN apk add wget
RUN wget https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz
RUN tar -xvzf sbt*.tgz
RUN mv sbt/bin/sbt /usr/bin/sbt

WORKDIR /app
COPY . /app

RUN sbt assembly

FROM eclipse-temurin:21.0.1_12-jre-alpine
WORKDIR /app

COPY --from=build /app/target/scala-*/*.jar /app/cron-resource.jar
COPY deploy/scripts/check /opt/resource/check
COPY deploy/scripts/in /opt/resource/in
COPY deploy/scripts/out /opt/resource/out