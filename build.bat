git checkout HEAD -- build.gradle
sed -i 's/BUILD_ID/dev/g' build.gradle
SET GRADLE_OPTS="-Dfile.encoding=UTF-8"
gradlew build --stacktrace
git checkout HEAD -- build.gradle
