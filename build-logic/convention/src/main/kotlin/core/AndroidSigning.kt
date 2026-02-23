package core

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.io.File
import java.util.Properties

internal data class AndroidSigningConfig(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

internal fun Project.loadAndroidSigningConfig(): AndroidSigningConfig? {
    val signingFile = rootProject.file("signing.properties")
    if (!signingFile.exists()) return null

    val props = Properties()
    runCatching { signingFile.inputStream().use(props::load) }.getOrElse { error ->
        logger.warn("[signing] Failed to load signing.properties: ${error.message}")
        return null
    }

    val storePath = props.getProperty("storeFile")
        ?: props.getProperty("signing.store.path")
        ?: props.getProperty("keystore.path")
    val storePassword = props.getProperty("storePassword")
        ?: props.getProperty("signing.store.password")
        ?: props.getProperty("keystore.password")
    val keyAlias = props.getProperty("keyAlias")
        ?: props.getProperty("signing.key.alias")
        ?: props.getProperty("key.alias")
    val keyPassword = props.getProperty("keyPassword")
        ?: props.getProperty("signing.key.password")
        ?: props.getProperty("key.password")

    if (storePath.isNullOrBlank() || storePassword.isNullOrBlank() || keyAlias.isNullOrBlank() || keyPassword.isNullOrBlank()) {
        logger.warn("[signing] Incomplete signing.properties: require storePath/storePassword/keyAlias/keyPassword")
        return null
    }

    val keyStoreFile = rootProject.file(storePath)
    if (!keyStoreFile.exists()) {
        logger.warn("[signing] Keystore path not found: ${keyStoreFile.absolutePath}")
        return null
    }

    return AndroidSigningConfig(
        storeFile = keyStoreFile,
        storePassword = storePassword,
        keyAlias = keyAlias,
        keyPassword = keyPassword,
    )
}

internal fun ApplicationExtension.applyReleaseSigningFrom(project: Project) {
    val signing = project.loadAndroidSigningConfig() ?: return

    signingConfigs {
        if (findByName("release") == null) {
            create("release") {
                storeFile = signing.storeFile
                storePassword = signing.storePassword
                keyAlias = signing.keyAlias
                keyPassword = signing.keyPassword
            }
        }
    }

    buildTypes.configureEach {
        if (name == "release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
