FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /opt/build

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN ./gradlew dependencies --no-daemon -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m"

COPY src src
RUN ./gradlew bootJar --no-daemon --no-build-cache -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m"

FROM eclipse-temurin:25-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /opt/build/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m"
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-jar", "app.jar"]