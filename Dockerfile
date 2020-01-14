FROM maven:3-jdk-11

RUN apt-get update && apt-get upgrade --yes && apt-get clean
EXPOSE 8080
ADD pom.xml /
ADD src /src
RUN mvn package
CMD mvn jetty:run
