// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.secrets.gradlePlugin)
        classpath("androidx.benchmark:benchmark-gradle-plugin:1.1.0-beta04")
    }

}

subprojects {
    group = "cn.qhplus.emo"
}