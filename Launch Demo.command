#!/bin/sh
# macOS convenience launcher; the game itself uses only Java/JDK classes.
cd "$(dirname "$0")" || exit 1
JAR="build/libs/ClassroomUprising-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
    printf '%s\n' 'Build once in this folder with: ./gradlew test jar'
    exit 1
fi
exec java -jar "$JAR" --gui
