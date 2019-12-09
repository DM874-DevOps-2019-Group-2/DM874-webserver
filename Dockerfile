FROM bigtruedata/sbt as build

COPY . /

WORKDIR /

RUN sbt test

RUN sbt dist

FROM openjdk:11-alpine

COPY --from=build /*.jar /webserver.jar

COPY --from=build /conf/application.conf /application.conf

ENTRYPOINT ["java", "-jar", "/webserver.jar"]
