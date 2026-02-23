plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.feature.proxy"

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data:proxy"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:client"))

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("io.insert-koin:koin-androidx-compose:4.1.1")
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("dev.chrisbanes.haze:haze:1.7.2")
    implementation("io.github.panpf.sketch4:sketch-compose:4.3.1")
    implementation("io.github.panpf.sketch4:sketch-http:4.3.1")

    val miuixVersion = "0.8.3"
    implementation("top.yukonga.miuix.kmp:miuix:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons:$miuixVersion")
}

