#!/bin/sh

git remote remove origin
git remote add origin https://github.com/Lythom/capsule.git
git pull origin ${COMMIT}
git checkout ${COMMIT}

GRADLE_OPTS="-Dfile.encoding=UTF-8"
./gradlew build --stacktrace
