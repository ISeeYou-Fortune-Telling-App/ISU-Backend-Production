# Build stage
FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /home/app
COPY pom.xml .
COPY src ./src
# Build and list the target directory to verify JAR creation
RUN mvn clean package -DskipTests=true && ls -la /home/app/target/

# Package stage
FROM eclipse-temurin:17-jre-focal
COPY --from=build /home/app/target/fortunetelling-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/local/lib/app.jar"]
