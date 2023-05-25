plugins {
    id("emo.android.library")
    id("emo.spotless")
    id("emo.publish")
}

version = version = libs.versions.emoTemplate.get()

android {
    namespace = "cn.qhplus.emo.template"
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
    implementation(libs.androidx.core.ktx)
}
