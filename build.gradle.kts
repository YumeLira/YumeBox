val agpVersion = "9.0.0"
val kotlinVersion = "2.3.0"
val composePluginVersion = "1.9.3"
val kspVersion = "2.3.3"
val aboutLibrariesVersion = "13.2.1"
val googleServicesVersion = "4.4.4"
val crashlyticsVersion = "3.0.6"

plugins {
    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    kotlin("android") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    kotlin("plugin.compose") version kotlinVersion apply false
    id("org.jetbrains.compose") version composePluginVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
    id("com.mikepenz.aboutlibraries.plugin") version aboutLibrariesVersion apply false
    id("com.google.gms.google-services") version googleServicesVersion apply false
    id("com.google.firebase.crashlytics") version crashlyticsVersion apply false
    id("dev.oom-wg.purejoy.fyl.fytxt") version "+" apply false
}
