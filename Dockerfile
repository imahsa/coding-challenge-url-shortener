# ---------- build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test

# Boot produces url-shortener-*-SNAPSHOT.jar and *-plain.jar — use the fat jar only
RUN cp "$(ls /app/build/libs/*-SNAPSHOT.jar | grep -v plain)" /app/app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/app.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
