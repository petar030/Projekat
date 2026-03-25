FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml ./
COPY src src
COPY uploads uploads
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=build /app/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
