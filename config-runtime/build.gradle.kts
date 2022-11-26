plugins {
    kotlin("jvm")
    id("emo.spotless")
    id("emo.publish")
}

version = libs.versions.emoConfig.get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
