import java.io.ByteArrayOutputStream

plugins {
    id("emo.android.application")
    id("emo.android.application.compose")
}

fun runCommand(project: Project, command: String): String {
    val stdout = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split(" ")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}


val gitVersion = runCommand(project, "git rev-list HEAD --count").toIntOrNull() ?: 1


android {


    defaultConfig {
        applicationId = "cn.qhplus.emo"
        versionCode = gitVersion
        versionName = "1.0.0"

        ndk {
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
//            signingConfig = signingConfigs.getByName("release")
        }
    }

    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    namespace = "cn.qhplus.emo"
}

dependencies {
    implementation(project(":ui-core"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.window.manager)
    implementation(libs.material3)

    implementation(libs.accompanist.navigation)
}
