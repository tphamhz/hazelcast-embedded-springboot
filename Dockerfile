FROM arm64v8/openjdk:21-ea-21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

