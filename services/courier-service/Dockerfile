FROM node:20-alpine AS build-frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend .
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline
COPY src ./src
COPY --from=build-frontend /app/frontend/dist/ src/main/resources/public/
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 7070
EXPOSE 5672

ENTRYPOINT ["java","-jar","app.jar"]
