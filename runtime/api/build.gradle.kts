plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.runtime.api"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
