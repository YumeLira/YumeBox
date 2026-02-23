plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.update"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":locale"))

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")

    val miuixVersion = "0.8.3"
    implementation("top.yukonga.miuix.kmp:miuix:$miuixVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.jakewharton.timber:timber:5.0.1")

    val updateVersion = "1.3.0-open"
    implementation("com.taobao.android:update-main:$updateVersion")
    implementation("com.taobao.android:update-common:$updateVersion")
    implementation("com.taobao.android:update-datasource:$updateVersion")
    implementation("com.taobao.android:update-adapter:$updateVersion")
}

