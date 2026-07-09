FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S shareway && adduser -S shareway -G shareway
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p uploads logs && chown -R shareway:shareway /app
USER shareway
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Xms256m", "-Dspring.profiles.active=prod", "app.jar"]
