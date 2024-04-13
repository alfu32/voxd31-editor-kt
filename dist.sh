#!/bin/bash

export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

tag=$1

echo $tag

echo "creating dist/desktop-jvm17-$tag.jar"

sdk use java 17.0.10-tem
./gradlew clean dist  -PjavaCompatVersion=17
mv desktop/build/libs/desktop-1.0.jar dist/desktop-jvm17-$tag.jar
git add dist/desktop-jvm17-$tag.jar

echo "creating dist/desktop-jvm8-$tag.jar"
sdk use java 8.0.402-tem
./gradlew clean dist  -PjavaCompatVersion=8
mv desktop/build/libs/desktop-1.0.jar dist/desktop-jvm8-$tag.jar
git add dist/desktop-jvm8-$tag.jar

sdk use java 17.0.10-tem
