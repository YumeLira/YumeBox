plugins {
    id("com.android.application") version "8.12.3" apply false
    id("com.android.library") version "8.12.3" apply false
    kotlin("android") version "2.2.21" apply false
    kotlin("multiplatform") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    kotlin("plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("com.google.devtools.ksp") version "2.3.3" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "13.1.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

buildscript {
    dependencies {
        classpath("dev.oom-wg.PureJoy-MultiLang:plugin:-SNAPSHOT")
    }
}