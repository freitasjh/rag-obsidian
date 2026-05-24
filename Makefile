JAR_NAME = mcp-runner.jar
JAVA_OPTS =

.PHONY: build run run-foreground clean docker-build docker-run docker-stop logs

build:
	mvn clean package -DskipTests
	ln -sf mcp-runner.jar target/mcp.jar

run-foreground:
	java $(JAVA_OPTS) -jar target/$(JAR_NAME)

run:
	java $(JAVA_OPTS) -jar target/$(JAR_NAME) &

clean:
	mvn clean

docker-build:
	docker build -t obsidian-rag .

docker-run:
	docker compose up -d

docker-stop:
	docker compose down

logs:
	docker compose logs -f
