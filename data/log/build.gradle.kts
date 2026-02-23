plugins {
    id("com.android.library")
    id("yumebox.base.android")
}

android {
    namespace = "com.github.yumelira.yumebox.data.log"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
