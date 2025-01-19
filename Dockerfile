FROM eclipse-temurin:17-alpine

ARG JAR_ARTIFACT=target/*.jar

RUN mkdir /app

COPY ${JAR_ARTIFACT} /app/currency-exchange.jar

CMD ["java", "-jar", "/app/currency-exchange.jar"]