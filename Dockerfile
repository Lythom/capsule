FROM java:8-jdk
MAINTAINER Samuel BOUCHET <contact@samuel-bouchet.fr>

ADD . /
RUN chmod +x build.sh gradlew

CMD ["sh","-x","build.sh"]

ENV COMMIT=master