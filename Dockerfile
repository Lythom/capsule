FROM java:8-jdk
MAINTAINER Samuel BOUCHET <contact@samuel-bouchet.fr>

ADD . /
RUN chmod +x build.sh gradlew
