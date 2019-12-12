FROM bigtruedata/sbt as build

COPY . /

WORKDIR /

RUN apt-get update && apt-get install -y git

RUN git clone https://github.com/ValdemarGr/sdis.git

RUN cd sdis && sbt +publishLocal

RUN sbt test

RUN sbt dist

FROM adoptopenjdk/openjdk11:jre

COPY --from=build /target/universal/webserver-1.0-SNAPSHOT.zip /webserver-1.0-SNAPSHOT.zip
COPY --from=build /conf/application.conf /application.conf

RUN apt-get update && apt-get install -y unzip

RUN unzip webserver-1.0-SNAPSHOT.zip && rm webserver-1.0-SNAPSHOT.zip

CMD ./webserver-1.0-SNAPSHOT/bin/webserver -Dconfig.file/application.conf -Dhttp.port=9000
