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
        classpath(libs.hilt.gradlePlugin)
        classpath(libs.secrets.gradlePlugin)
    }

}

subprojects {
    group = "cn.qhplus.emo"
}