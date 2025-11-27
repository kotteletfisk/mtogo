
# 1. legacy Rust system
FROM rust:1.91.1 AS rust-build

WORKDIR /app/legacy-system
COPY legacy-system/Cargo.toml .
COPY legacy-system/src ./src
COPY legacy-system/assets ./assets

RUN cargo build --release

# 2. Svelte frontend for Java application
FROM node:alpine AS node-build
ENV GCP_BUILDPACKS=1
WORKDIR /app
COPY frontend/package*.json ./
RUN npm i -D @sveltejs/adapter-static
RUN npm ci
COPY frontend/ ./
RUN npm run build-prod


# 3. Java application
FROM maven:3.9.6-eclipse-temurin-21 AS java-build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# copy Svelte build output into Java resources
RUN mkdir -p src/main/resources/public
COPY --from=node-build /app/build ./src/main/resources/public

RUN mvn -B -DskipTests package


# 3. runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy Java JAR
COPY --from=java-build /app/target/supplier-service.jar /app/app.jar

# Copy Rust binary
COPY --from=rust-build /app/legacy-system/target/release/legacy-system /app/legacy-system
COPY --from=rust-build /app/legacy-system/assets ./assets


# Copy entrypoint script
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 7070
EXPOSE 8080
EXPOSE 5672

ENTRYPOINT ["/app/start.sh"]
