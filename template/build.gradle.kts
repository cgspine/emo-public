plugins {
    id("emo.android.library")
    id("emo.spotless")
    id("emo.publish")
}

version = "0.0.1"

android {
    namespace = "cn.qhplus.emo.template"
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
}
