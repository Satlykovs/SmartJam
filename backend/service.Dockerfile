FROM gradle:9.5.1-jdk25-alpine AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle backend/gradlew .
COPY --chown=gradle:gradle backend/gradle gradle
COPY --chown=gradle:gradle backend/build.gradle .
COPY --chown=gradle:gradle backend/settings.gradle .

RUN ./gradlew --version --no-daemon

COPY --chown=gradle:gradle openapi-spec /home/gradle/openapi-spec
COPY --chown=gradle:gradle backend/ .

RUN ./gradlew :smartjam-common:compileJava --no-daemon

ARG SERVICE_NAME

ENV GRADLE_USER_HOME=/home/gradle/.gradle-$SERVICE_NAME

RUN ./gradlew spotlessApply :${SERVICE_NAME}:build -x test --no-daemon \
    -Dorg.gradle.vfs.watch=false

FROM eclipse-temurin:25-jre-alpine AS layers
WORKDIR /extract
ARG SERVICE_NAME
COPY --from=build /home/gradle/src/${SERVICE_NAME}/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:25-jre-alpine
WORKDIR /application
ARG SERVICE_NAME


RUN if [ "$SERVICE_NAME" = "smartjam-analyzer" ]; then \
        apk add --no-cache ffmpeg; \
    fi && \
    addgroup -S smartjam && adduser -S smartjam -G smartjam

USER smartjam

COPY --from=layers /extract/dependencies/ ./
COPY --from=layers /extract/spring-boot-loader/ ./
COPY --from=layers /extract/snapshot-dependencies/ ./
COPY --from=layers /extract/application/ ./

ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseSerialGC", \
            "-Xss256k", \
            "org.springframework.boot.loader.launch.JarLauncher"]