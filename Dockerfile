FROM alpine:3.13.1 AS build

# build with Java 8, where the code was written in
RUN apk add bash git openssl \
    openjdk8 maven

RUN mkdir -p /app

WORKDIR /app

COPY . /app/ISLE2CLARIN

RUN if [ ! -f /app/ISLE2CLARIN/pom.xml ]; then rm -rf ISLE2CLARIN; git clone https://github.com/TheLanguageArchive/ISLE2CLARIN.git; fi
RUN cd /app/ISLE2CLARIN && \
    MAVEN_OPTS=-Xss10M mvn install

FROM alpine:3.13.1
# run with Java 11, which listens to container limits
RUN apk add openjdk11-jre

RUN mkdir -p /app

WORKDIR /app

COPY --from=build /app/ISLE2CLARIN/target/isle2clarin.jar /app/

ENTRYPOINT ["java","-jar", "/app/isle2clarin.jar"] 