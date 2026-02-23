import core.mmkvDependencyNotation

plugins {
    id("com.android.library")
    id("yumebox.base.android")
}

val mmkvDependency = project.mmkvDependencyNotation()

android {
    namespace = "com.github.yumelira.yumebox.core.di"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(project(":data:log"))
    implementation(project(":data:proxy"))
    implementation(project(":data:settings"))
    implementation(project(":runtime:client"))

    implementation(mmkvDependency)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
}
