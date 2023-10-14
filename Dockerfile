FROM ubuntu:18.04
MAINTAINER Roman Shishkin <romashkin.2001@yandex.ru>

#Updating system and installing packages
WORKDIR /
ARG DEBIAN_FRONTEND=noninteractive
RUN echo 'APT::Install-Recommends "0";' > /etc/apt/apt.conf.d/99norecommends \
    && apt-get -y update \
    && apt-get -y install \
    apt-utils \
    default-jdk \
    wget \
    curl \
    libpulse0 \
    locales \
    libxkbcommon-x11-0 \
    && apt-get autoremove -y \
    && apt-get clean -y

RUN echo 'ru_RU.UTF-8 UTF-8' > /etc/locale.gen \
   && locale-gen
ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_LANG ru_RU.UTF-8
ENV LC_ALL ru_RU.UTF-8

#Setting directories args
ARG APP_DIR=web-client
ARG CONFIGS_DIR=config

#Copying configs
WORKDIR /$APP_DIR/$CONFIGS_DIR
ARG APP_PROPS_FILE=src/main/resources/application.yml
ARG PROPS=application.yml
COPY $APP_PROPS_FILE $PROPS

#Copying application
WORKDIR /$APP_DIR
ARG JAR_FILE=build/libs/trik-testsys-web-client-1.1.0.3.jar
ARG APP=app.jar
COPY $JAR_FILE $APP

#Running application
EXPOSE 8888
ENTRYPOINT java -jar -Dspring.config.location=/$APP_DIR/$CONFIGS_DIR app.jar