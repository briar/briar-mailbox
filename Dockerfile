FROM eclipse-temurin:11 AS build
WORKDIR /mailbox
COPY . /mailbox
RUN ./gradlew x86LinuxJar 


FROM eclipse-temurin:11
RUN mkdir -p /root/.local/share
VOLUME /root
WORKDIR /mailbox
COPY --from=build /mailbox/mailbox-cli/build/libs/mailbox-cli-linux-x86_64.jar /mailbox/mailbox-cli-linux-x86_64.jar
CMD [ "java", "-jar", "/mailbox/mailbox-cli-linux-x86_64.jar" ]
