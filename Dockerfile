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
    echo 'Targetarch is not supported. Exiting.' ; \
    exit 1 ; \
    fi;

FROM eclipse-temurin:17
RUN mkdir -p /root/.local/share
VOLUME /root
WORKDIR /mailbox
COPY --from=build /mailbox/mailbox-cli/build/libs/mailbox-cli-linux.jar /mailbox/mailbox-cli-linux.jar
CMD [ "java", "-jar", "/mailbox/mailbox-cli-linux.jar" ]
