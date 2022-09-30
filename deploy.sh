#!/usr/bin/env bash

#./deploy.sh core publishToMavenLocal
#./deploy.sh ui-core publishToMavenLocal
#./deploy.sh fs publishToMavenLocal
#./deploy.sh modal publishToMavenLocal
#./deploy.sh photo publishToMavenLocal
#./deploy.sh network publishToMavenLocal
#./deploy.sh permission publishToMavenLocal
#./deploy.sh all publishToMavenLocal

#./deploy.sh core publish
#./deploy.sh ui-core publish
#./deploy.sh fs publish
#./deploy.sh modal publish
#./deploy.sh photo publish
#./deploy.sh network publish
#./deploy.sh permission publish
#./deploy.sh all publish

buildCore="./gradlew :core:clean :core:build core:$2"
buildUiCore="./gradlew :ui-core:clean :ui-core:build :ui-core:$2"
buildFs="./gradlew :fs:clean :fs:build :fs:$2"
buildModal="./gradlew :modal:clean :modal:build :modal:$2"
buildPhoto="./gradlew :photo:clean :photo:build :photo:$2"
buildPhotoCoil="./gradlew :photo-coil:clean :photo-coil:build :photo-coil:$2"
buildNetwork="./gradlew :network:clean :network:build :network:$2"
buildPermission="./gradlew :permission:clean :permission:build :permission:$2"


if [[ "core" == "$1" ]]
then
    $buildCore
elif [[ "ui-core" == "$1" ]]
then
    $buildUiCore
elif [[ "fs" == "$1" ]]
then
    $buildFs
elif [[ "modal" == "$1" ]]
then
    $buildModal
elif [[ "network" == "$1" ]]
then
    $buildNetwork
elif [[ "permission" == "$1" ]]
then
    $buildPermission
elif [[ "photo" == "$1" ]]
then
    $buildPhoto
    $buildPhotoCoil
elif [[ "all" == "$1" ]]
then
    $buildCore
    $buildUiCore
    $buildFs
    $buildNetwork
    $buildPermission
    $buildModal
    $buildPhoto
    $buildPhotoCoil
fi