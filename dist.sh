#!/bin/bash

export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

tag=$1

echo $tag
# Define a list of version strings
versions="8.0.402-tem 11.0.22-tem 17.0.10-tem 21.0.2-tem"
rm -rf ./dist/*.jar
# Iterate over the list of versions
for jdk_release in $versions
do
    # Extract the major version number (the first number before the dot)
    major_version="${jdk_release%%.*}"
    echo "creating dist/voxd31-editor-desktop-jvm$major_version-$tag.jar using sdk release $jdk_release"
    sdk use java "$jdk_release"
    ./gradlew clean dist  "-PjavaCompatVersion=$major_version" -PreleaseNumber=$tag
    mv desktop/build/libs/*.jar dist/
    ## git add -f dist/voxd31-editor-desktop-jvm$major_version-$tag.jar
done


cp scripts/voxd31-* ./dist/
