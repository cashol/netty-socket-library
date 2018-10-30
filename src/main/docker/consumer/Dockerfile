FROM openjdk:11.0.1-jre-slim
MAINTAINER Holger Casties "holger.casties@siemens.com"
COPY *.jar /app/netty-socket-library-consumer.jar
ENTRYPOINT ["java"]
# Enable JAVA_OPTS on container
ENTRYPOINT exec java $JAVA_OPTS -jar /app/netty-socket-library-consumer.jar
