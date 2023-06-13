#!/usr/bin/env bash

#./deploy.sh core publishToMavenLocal
#./deploy.sh ui-core publishToMavenLocal
#./deploy.sh fs publishToMavenLocal
#./deploy.sh modal publishToMavenLocal
#./deploy.sh photo publishToMavenLocal
#./deploy.sh network publishToMavenLocal
#./deploy.sh permission publishToMavenLocal
#./deploy.sh js-bridge publishToMavenLocal
#./deploy.sh report publishToMavenLocal
#./deploy.sh config publishToMavenLocal
#./deploy.sh scheme publishToMavenLocal
#./deploy.sh kv publishToMavenLocal
#./deploy.sh device publishToMavenLocal
#./deploy.sh all publishToMavenLocal

#./deploy.sh core publish
#./deploy.sh ui-core publish
#./deploy.sh fs publish
#./deploy.sh modal publish
#./deploy.sh photo publish
#./deploy.sh network publish
#./deploy.sh permission publish
#./deploy.sh js-bridge publish
#./deploy.sh report publish
#./deploy.sh config publish
#./deploy.sh scheme publish
#./deploy.sh kv publish
#./deploy.sh device publish
#./deploy.sh all publish

buildCore="./gradlew.bat :core:clean :core:build core:$2"
buildUiCore="./gradlew.bat :ui-core:clean :ui-core:build :ui-core:$2"
buildFs="./gradlew.bat :fs:clean :fs:build :fs:$2"
buildModal="./gradlew.bat :modal:clean :modal:build :modal:$2"
buildPhoto="./gradlew.bat :photo:clean :photo:build :photo:$2"
buildPhotoCoil="./gradlew.bat :photo-coil:clean :photo-coil:build :photo-coil:$2"
buildPhotoPdf="./gradlew.bat :photo-pdf:clean :photo-pdf:build :photo-pdf:$2"
buildNetwork="./gradlew.bat :network:clean :network:build :network:$2"
buildPermission="./gradlew.bat :permission:clean :permission:build :permission:$2"
buildJsBridge="./gradlew.bat :js-bridge:clean :js-bridge:build :js-bridge:$2"
buildReport="./gradlew.bat :report:clean :report:build :report:$2"
buildDevice="./gradlew.bat :device:clean :device:build :device:$2"
buildConfigRuntime="./gradlew.bat :config-runtime:clean :config-runtime:build :config-runtime:$2"
buildConfigMMKV="./gradlew.bat :config-mmkv:clean :config-mmkv:build :config-mmkv:$2"
buildConfigKsp="./gradlew.bat :config-ksp:clean :config-ksp:build :config-ksp:$2"
buildConfigPanel="./gradlew.bat :config-panel:clean :config-panel:build :config-panel:$2"
buildSchemeRuntime="./gradlew.bat :scheme-runtime:clean :scheme-runtime:build :scheme-runtime:$2"
buildSchemeKsp="./gradlew.bat :scheme-ksp:clean :scheme-ksp:build :scheme-ksp:$2"
buildSchemeImpl="./gradlew.bat :scheme-impl:clean :scheme-impl:build :scheme-impl:$2"
buildKv="./gradlew.bat :kv:clean :kv:build :kv:$2"

./gradlew.bat :core:spotlessApply :ui-core:spotlessApply :fs:spotlessApply \
:photo:spotlessApply :photo-coil:spotlessApply :photo-pdf:spotlessApply \
:modal:spotlessApply :network:spotlessApply :permission:spotlessApply \
:js-bridge:spotlessApply :report:spotlessApply :device:spotlessApply \
:config-runtime:spotlessApply :config-mmkv:spotlessApply :config-ksp:spotlessApply :config-panel:spotlessApply \
:scheme-runtime:spotlessApply :scheme-ksp:spotlessApply :scheme-impl:spotlessApply \
:kv:spotlessApply

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
elif [[ "js-bridge" == "$1" ]]
then
    $buildJsBridge
elif [[ "photo" == "$1" ]]
then
    $buildPhoto
    $buildPhotoCoil
    $buildPhotoPdf
elif [[ "report" == "$1" ]]
then
    $buildReport
elif [[ "device" == "$1" ]]
then
    $buildDevice
elif [[ "config" == "$1" ]]
then
    $buildConfigRuntime
    $buildConfigKsp
    $buildConfigMMKV
    $buildConfigPanel
elif [[ "scheme" == "$1" ]]
then
    $buildSchemeRuntime
    $buildSchemeKsp
    $buildSchemeImpl
elif [[ "kv" == "$1" ]]
then
    $buildKv
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
    $buildPhotoPdf
    $buildJsBridge
    $buildReport
    $buildConfigRuntime
    $buildConfigKsp
    $buildConfigMMKV
    $buildConfigPanel
    $buildSchemeRuntime
    $buildSchemeKsp
    $buildSchemeImpl
    $buildKv
    $buildDevice
fi