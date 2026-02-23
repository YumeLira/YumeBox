package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class DownloadGeoFilesTask : DefaultTask() {
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
                URI(url).toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }
}
