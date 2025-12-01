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
            packageName = "plus.yumeyuka.yumebox.generated"
            useKotlin = true
            isRestrictedAccessEnabled = false
            isIsolationEnabled = true
        }
    }
    projects(":core", ":extension") {
        android { isEnabled = false }
    }
}

include(":core", ":extension", ":app")
