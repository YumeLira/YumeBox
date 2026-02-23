plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.core.ui"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":platform"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:api"))

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("dev.chrisbanes.haze:haze:1.7.2")

    val miuixVersion = "0.8.3"
    implementation("top.yukonga.miuix.kmp:miuix:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons:$miuixVersion")
}

