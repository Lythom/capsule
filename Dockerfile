FROM java:8-jdk
MAINTAINER Samuel BOUCHET <contact@samuel-bouchet.fr>

ADD . /
RUN chmod +x build.sh gradlew

# Standard SSH port
EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]

ENV COMMIT=master