package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class DownloadGeoFilesTask : DefaultTask() {
    companion object {
        private val ALLOWED_SCHEMES = setOf("https")
        private val ALLOWED_HOSTS = setOf("github.com")
    }

    @get:Input
    abstract val assetUrls: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val destinationDir = outputDirectory.get().asFile
        destinationDir.mkdirs()

        assetUrls.get().forEach { (fileName, url) ->
            val outputFile = destinationDir.resolve(fileName)
            runCatching {
                val validatedUri = validateDownloadUri(url)
                validatedUri.toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }

    private fun validateDownloadUri(rawUrl: String): URI {
        val uri = try {
            URI(rawUrl)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Invalid download URL: $rawUrl", e)
        }

        val scheme = uri.scheme?.lowercase()
        require(scheme in ALLOWED_SCHEMES) {
            "Unsupported download URL scheme '$scheme', only https is allowed"
        }
        require(uri.userInfo == null) {
            "User info is not allowed in download URL"
        }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "Query/fragment is not allowed in download URL"
        }

        val host = uri.host?.lowercase()
        require(!host.isNullOrBlank()) {
            "Download URL host is missing"
        }
        require(host in ALLOWED_HOSTS) {
            "Download URL host '$host' is not in allowlist"
        }
        require(uri.port == -1 || uri.port == 443) {
            "Only default HTTPS port is allowed"
        }

        return uri
    }
}
