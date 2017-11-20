git checkout HEAD -- build.properties
sed -i 's/BUILD_ID/dev/g' build.properties
SET GRADLE_OPTS="-Dfile.encoding=UTF-8"
gradlew build --stacktrace
git checkout HEAD -- build.properties
