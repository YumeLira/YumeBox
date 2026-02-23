import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.data.settings"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":locale"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // Keep MMKV as compileOnly here, but version must match app/runtime modules per ABI.
    compileOnly(mmkvDependency)
}

