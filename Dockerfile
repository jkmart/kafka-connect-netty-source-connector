FROM maven:3.8.1-adoptopenjdk-8 as builder
WORKDIR /app
COPY . .
RUN mvn package -P confluent-hub

FROM confluentinc/cp-server-connect-base:6.1.2 as runtime

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

USER root
COPY --from=builder --chown=appuser:appuser /app/target/components/packages/jkmart-netty-source-1.0.0.zip jkmart-netty-source-1.0.0.zip
USER appuser

RUN confluent-hub install --no-prompt ./jkmart-netty-source-1.0.0.zip
