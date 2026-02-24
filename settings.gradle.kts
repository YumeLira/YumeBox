@file:Suppress("UnstableApiUsage")


rootProject.name = "YumeBox"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/nexus/content/repositories/releases/")

        maven("https://oom-maven.sawahara.host") {
            content {
                includeGroupAndSubgroups("ren.shiror")
                includeGroupAndSubgroups("work.niggergo")
                includeGroupAndSubgroups("dev.oom-wg")
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
        maven ("https://maven.aliyun.com/nexus/content/repositories/releases/")

        maven("https://oom-maven.sawahara.host") {
            content {
                includeGroupAndSubgroups("ren.shiror")
                includeGroupAndSubgroups("work.niggergo")
                includeGroupAndSubgroups("dev.oom-wg")
            }
        }
        maven("https://maven.kr328.app/releases") // KAIDL 官方仓库
    }

    versionCatalogs {
        create("libs")
    }
}

plugins {
    id("com.highcapable.gropify") version "1.0.1"
}

gropify {
    isEnabled = true
    global {
        common {
            isEnabled = true
            useTypeAutoConversion = true
            useValueInterpolation = true
            existsPropertyFiles("gradle.properties", addDefault = false)
            excludeKeys(
                "signing.store.password",
                "signing.key.password",
                "signing.store.path",
                "signing.key.alias",
            )
        }
        android {
            generateDirPath = "build/generated/gropify"
            sourceSetName = "main"
            packageName = "com.github.yumelira.yumebox.yumebox.generated"
            useKotlin = true
            isRestrictedAccessEnabled = false
            isIsolationEnabled = true
        }
    }
    projects(":core", ":extension") {
        android { isEnabled = false }
    }
}

include(
    ":core",
    ":platform",
    ":common",
    ":di",
    ":locale",
    ":ui",
    ":extension",
    ":app",
    ":feature:update",
    ":feature:web",
    ":feature:substore",
    ":feature:proxy",
    ":data:log",
    ":data:settings",
    ":data:proxy",
    ":runtime:api",
    ":runtime:client",
    ":runtime:service",
)
