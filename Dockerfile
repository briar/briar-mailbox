# syntax=docker/dockerfile:1

FROM eclipse-temurin:17 AS build
WORKDIR /mailbox
COPY . /mailbox
ARG TARGETARCH
RUN if [ "$TARGETARCH" = "arm64" ]; then \
    ./gradlew aarch64LinuxJar ; \
    mv mailbox-cli/build/libs/mailbox-cli-linux-aarch64.jar mailbox-cli/build/libs/mailbox-cli-linux.jar ; \
    elif [ "$TARGETARCH" = "amd64" ]; then \
    ./gradlew x86LinuxJar ; \
    mv mailbox-cli/build/libs/mailbox-cli-linux-x86_64.jar mailbox-cli/build/libs/mailbox-cli-linux.jar ; \
    else \
    echo 'The target architecture is not supported. Exiting.' ; \
    exit 1 ; \
    fi;

FROM eclipse-temurin:17
RUN mkdir -p /root/.local/share
VOLUME /root
WORKDIR /mailbox
COPY --from=build /mailbox/mailbox-cli/build/libs/mailbox-cli-linux.jar /mailbox/mailbox-cli-linux.jar
CMD [ "java", "-jar", "/mailbox/mailbox-cli-linux.jar" ]


FROM ghcr.io/linuxserver/baseimage-alpine:latest

# set version label
ARG BUILD_DATE
ARG VERSION
LABEL build_version="Linuxserver.io version:- ${VERSION} Build-date:- ${BUILD_DATE}"
LABEL maintainer="smhrambo"

RUN \
  echo "**** install dependencies ****" && \
  apk upgrade && \
  apk add --no-cache \
    openjdk-17-jre-headless && \
  echo "**** cleanup ****" && \
  rm -rf \
    /tmp/*

# add bin files
COPY --from=build /mailbox/mailbox-cli/build/libs/mailbox-cli-linux.jar /usr/lib/briar/mailbox-cli-linux.jar
# add local files
COPY root/ /

# Volumes and Ports
VOLUME /config
