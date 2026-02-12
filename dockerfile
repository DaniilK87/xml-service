FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/xml-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]