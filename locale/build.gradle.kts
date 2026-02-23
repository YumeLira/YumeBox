plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("yumebox.base.android")
    id("dev.oom-wg.purejoy.fyl.fytxt")
}

fytxt {
    langSrcs = mapOf(
        "lang" to layout.projectDirectory.dir("lang"),
    )
    packageName = "dev.oom_wg.purejoy.mlang"
    objectName = "MLang"
    defaultLang = "ZH"
    composeGen = true
    internalClass = false
}

android {
    namespace = "com.github.yumelira.yumebox.core.locale"

    sourceSets {
        named("main") {
            kotlin.srcDir("build/generated/fytxt/kotlin/commonMain/kotlin")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
}
