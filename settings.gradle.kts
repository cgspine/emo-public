pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}
rootProject.name = "emo"
include(":core")
include(":fs")
include(":ui-core")
include(":network")
include(":js-bridge")
include(":modal")
include(":photo")
include(":photo-coil")
include(":photo-pdf")
include(":permission")
include(":report")
include(":config-runtime")
include(":config-ksp")
include(":config-mmkv")
include(":config-panel")
include(":scheme-runtime")
include(":scheme-impl")
include(":scheme-ksp")
include(":kv")
include(":device")
include(":app")




//include(":benchmark")
//include(":microbenchmark")
