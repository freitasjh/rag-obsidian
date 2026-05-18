FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/quarkus-app/lib/ ./lib/
COPY target/quarkus-app/app/ ./app/
COPY target/quarkus-app/quarkus/ ./quarkus/
COPY target/quarkus-app/*.jar ./

ENV OBSIDIAN_VAULT_PATH=/data/vault
ENV OBSIDIAN_VAULT_NAME=brain
ENV RAG_CHUNK_SIZE=1000
ENV RAG_CHUNK_OVERLAP=200
ENV RAG_MAX_RESULTS=10

CMD ["java", "-jar", "quarkus-run.jar"]