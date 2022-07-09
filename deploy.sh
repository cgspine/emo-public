#!/usr/bin/env bash

#./deploy.sh core publishToMavenLocal
#./deploy.sh ui-core publishToMavenLocal
#./deploy.sh fs publishToMavenLocal
#./deploy.sh modal publishToMavenLocal
#./deploy.sh photo publishToMavenLocal

#./deploy.sh core publish
#./deploy.sh ui-core publish
#./deploy.sh fs publish
#./deploy.sh modal publish
#./deploy.sh photo publish

if [[ "core" == "$1" ]]
then
    buildCmd="./gradlew :core:clean :core:build core:$2"
    $buildCmd
elif [[ "ui-core" == "$1" ]]
then
    buildCmd="./gradlew :ui-core:clean :ui-core:build :ui-core:$2"
    $buildCmd
elif [[ "fs" == "$1" ]]
then
    buildCmd="./gradlew :fs:clean :fs:build :fs:$2"
    $buildCmd
elif [[ "modal" == "$1" ]]
then
    buildCmd="./gradlew :modal:clean :modal:build :modal:$2"
    $buildCmd
elif [[ "photo" == "$1" ]]
then
    buildCmd="./gradlew :photo:clean :photo:build :photo:$2"
    $buildCmd
    buildCmd="./gradlew :photo-coil:clean :photo-coil:build :photo-coil:$2"
    $buildCmd
fi