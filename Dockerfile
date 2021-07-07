FROM maven:3.8.1-adoptopenjdk-8 as builder
WORKDIR /app
COPY . .
RUN mvn package

FROM adoptopenjdk:8
RUN mkdir -p /opt/app

# TODO: update for this connect project
COPY --from=builder /app/target/kstreams-quickstart-archetype-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/kstreams-quickstart-archetype-1.0-SNAPSHOT.jar
RUN chown 1000:1000 /opt/app/kstreams-quickstart-archetype-1.0-SNAPSHOT.jar
USER 1000
CMD ["java", "-jar", "/opt/app/kstreams-quickstart-archetype-1.0-SNAPSHOT.jar"]