plugins {
    id("emo.android.library")
    id("emo.android.library.compose")
    id("emo.spotless")
    id("emo.publish")
}

version = libs.versions.emoConfig.get()

android {
    namespace = "cn.qhplus.emo.config.panel"
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":ui-core"))
    implementation(project(":config-runtime"))
}
