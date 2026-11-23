FROM eclipse-temurin:25-jdk
LABEL maintainer="Samuel BOUCHET <contact@samuel-bouchet.fr>"

ADD . /
RUN chmod +x build.sh gradlew
