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
        classpath(libs.benchmark.gradlePlugin)
    }

}

subprojects {
    group = "cn.qhplus.emo"
}