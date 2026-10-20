FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
ENV MAVEN_OPTS="-Xmx512m -XX:ActiveProcessorCount=2"
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system roomly && useradd --system --gid roomly roomly \
    && mkdir -p /app/media && chown roomly:roomly /app/media
WORKDIR /app
COPY --from=build /workspace/target/roomly.jar app.jar
ENV ROOMLY_BIND_ADDRESS=0.0.0.0
ENV ROOMLY_MEDIA_DIRECTORY=/app/media
USER roomly
EXPOSE 18092
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl -fsS --max-time 3 "http://127.0.0.1:${ROOMLY_HTTP_PORT:-18092}/actuator/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
