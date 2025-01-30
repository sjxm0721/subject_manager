# Docker 镜像构建
# @author <a href="https://github.com/sjxm0721">四季夏目</a>

FROM openjdk:8-jdk-alpine

RUN apt-get update && apt-get install -y \
    ffmpeg \
    libavcodec-dev \
    libavformat-dev \
    libavutil-dev \
    libswscale-dev \
    libtbb2 \
    libtbb-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/*.jar app.jar
COPY src/main/resources/application.yml /app/config/
COPY src/main/resources/application-prod.yml /app/config/
ENV SPRING_CONFIG_LOCATION=file:/app/config/
ENV TZ=Asia/Shanghai
EXPOSE 8101
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]