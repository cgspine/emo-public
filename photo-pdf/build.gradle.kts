plugins {
    id("emo.android.library")
    id("emo.android.library.compose")
    id("emo.spotless")
    id("emo.publish")
}

version = libs.versions.emoPhoto.get()

android {
    namespace = "cn.qhplus.emo.photo.pdf"
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }

        getByName("debug") {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }
}
dependencies {
    api(project(":photo"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.accompanist.systemuicontroller)
}
