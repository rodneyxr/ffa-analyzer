FROM docker.io/library/amazoncorretto:8-alpine-jdk as builder
WORKDIR /home/gradle
COPY . .
RUN ./gradlew installDist

FROM docker.io/library/amazoncorretto:8-alpine-jre
WORKDIR /ffa
COPY --from=builder /home/gradle/build/install/ffa-analyzer/ /usr/local/
ENTRYPOINT ["ffa-analyzer"]
CMD ["-f", "/ffa"]