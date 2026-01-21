FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-client-app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
