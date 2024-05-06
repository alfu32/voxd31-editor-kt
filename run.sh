#!/bin/bash

latest_tag=$(git describe --tags $(git rev-list --tags --max-count=1))

BUILD_RUNDATE=$(date '+%Y-%m-%d %H:%M:%S')
GIT_COMMIT=$(git rev-parse HEAD)
GIT_BRANCH="$(git branch | egrep "^\* ")"
GIT_TAG=$(git describe --tags $(git rev-list --tags --max-count=1))

export VERSIONFILE=core/src/main/kotlin/com/voxd31/editor/Voxd31EditorVersion.kt
cat > $VERSIONFILE <<VERSIONCLASS
package com.voxd31.editor

data class Voxd31EditorVersion(
  val buildGitCommit:String = "$GIT_COMMIT",
  val buildDate:String = "$BUILD_RUNDATE",
  val buildGitBranch:String = "${GIT_BRANCH/\*\ /}",
  val buildGitTag:String = "${GIT_TAG}",
  val buildVersion:String = "$latest_tag-dev"
)
VERSIONCLASS


./gradlew dist  "-PjavaCompatVersion=17" -PreleaseNumber="$latest_tag-dev"
mv desktop/build/libs/*.jar dist/
cp ./assets/voxd31.icon.png  dist/
java -jar "./dist/voxd31-editor-desktop-jvm17-$latest_tag-dev.jar" 1900x1000 "test-$latest_tag-dev.vxdi"