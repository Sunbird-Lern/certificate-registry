FROM sunbird/openjdk-java11-alpine:latest
RUN apk update \
    && apk add  unzip \
    && apk add curl \
    && adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird/ 
ADD ./service-1.0.0-SNAPSHOT-dist.zip /home/sunbird/
RUN unzip /home/sunbird/service-1.0.0-SNAPSHOT-dist.zip -d /home/sunbird/
RUN chown -R sunbird:sunbird /home/sunbird
USER sunbird
EXPOSE 9000
WORKDIR /home/sunbird/
CMD java -XX:+PrintFlagsFinal $JAVA_OPTIONS -cp '/home/sunbird/service-1.0.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart  /home/sunbird/service-1.0.0-SNAPSHOT
