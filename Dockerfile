FROM openjdk:8u212-jdk

# MAVEN
RUN apt-get update \
     && apt-get install -y maven \
     && apt-get clean \
     && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY . /usr/src/app
RUN cd /usr/src/app \
     && mvn clean package

EXPOSE 8080 2552

COPY entrypoint.sh /usr/local/bin
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]
