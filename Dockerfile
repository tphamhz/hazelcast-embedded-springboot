FROM arm64v8/openjdk:21-ea-21-jdk-slim
WORKDIR /app
COPY target/hazelcast-embedded-springboot*.jar app.jar
COPY target/hazelcast-replacer-filtered.jar hazelcast-ext/
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

