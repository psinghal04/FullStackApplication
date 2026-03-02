FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/dependencies/ ./
COPY --from=build /workspace/spring-boot-loader/ ./
COPY --from=build /workspace/snapshot-dependencies/ ./
COPY --from=build /workspace/application/ ./

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=30s \
  CMD wget -qO- http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]