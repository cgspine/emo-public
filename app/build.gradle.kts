/*
 * Copyright 2022 emo Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.io.ByteArrayOutputStream


plugins {
    id("emo.android.application")
    id("emo.android.application.compose")
    id("emo.spotless")
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

val gitVersion = providers.exec {
    commandLine("git", "rev-list", "HEAD", "--count")
}.standardOutput.asText.get().trim().toInt()


android {

    signingConfigs {
        val properties = gradleLocalProperties(project.rootDir)
        create("release"){
            keyAlias = properties.getProperty("sign_alias")
            keyPassword = properties.getProperty("sign_key_password")
            storeFile = project.rootProject.file("emo.keystore")
            storePassword = properties.getProperty("sign_store_password")
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "cn.qhplus.emo"
        versionCode = gitVersion
        versionName = "1.0.0"

        ndk {
//            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    applicationVariants.all {
        sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    namespace = "cn.qhplus.emo"
}

dependencies {
    implementation(project(":ui-core"))
    implementation(project(":photo-coil"))
    implementation(project(":modal"))
    implementation(project(":network"))
    implementation(project(":permission"))
    implementation(project(":js-bridge"))
    implementation(project(":report"))
    implementation(project(":config-mmkv"))
    implementation(project(":config-panel"))
    ksp(project(":config-ksp"))

    implementation(project(":scheme-impl"))
    ksp(project(":scheme-ksp"))

    implementation(project(":kv"))
    implementation(project(":device"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.window.manager)

    implementation(libs.accompanist.navigation)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.webview)

    implementation(libs.kotlinx.serialization.protobuf)
}
