FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
CMD ["./gradlew", "clean", "bootRun", "--no-daemon"]