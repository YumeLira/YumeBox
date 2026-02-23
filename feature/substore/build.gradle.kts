plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.substore"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":platform"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data:settings"))

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("io.insert-koin:koin-androidx-compose:4.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.caoccao.javet:javet-node-android:5.0.4")
    implementation("com.jakewharton.timber:timber:5.0.1")

    val miuixVersion = "0.8.3"
    implementation("top.yukonga.miuix.kmp:miuix:$miuixVersion")
}

