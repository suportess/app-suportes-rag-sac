FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY app/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY app/src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
