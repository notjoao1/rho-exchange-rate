FROM eclipse-temurin:17-alpine

RUN mkdir /app

WORKDIR /app

CMD ["./mvnw", "spring-boot:run"]
