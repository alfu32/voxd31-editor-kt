#!/bin/bash

latest_tag=$(git describe --tags $(git rev-list --tags --max-count=1))

./gradlew dist  "-PjavaCompatVersion=17" -PreleaseNumber="$latest_tag-dev"
mv desktop/build/libs/*.jar dist/
java -jar "./dist/voxd31-editor-desktop-jvm17-$latest_tag-dev.jar" 1900x1000 "test-$latest_tag-dev.vxdi"