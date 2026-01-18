package plugins

import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
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
                val uri = URI(url)
                uri.toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }
}

abstract class GeoAssetsExtension {
    abstract val assetUrls: MapProperty<String, String>
    abstract val outputDirectory: DirectoryProperty
}

class GeoAssetsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<GeoAssetsExtension>("geoAssets")
        extension.outputDirectory.convention(target.layout.projectDirectory.dir("src/androidMain/assets"))

        val downloadTask = target.tasks.register<DownloadGeoFilesTask>("downloadGeoFiles") {
            description = "Download GeoIP and GeoSite databases from MetaCubeX"
            group = "build setup"
            assetUrls.set(extension.assetUrls)
            outputDirectory.set(extension.outputDirectory)
        }

        target.tasks.register<Delete>("cleanGeoFiles") {
            description = "Clean downloaded GeoIP and GeoSite databases"
            group = "build setup"
            delete(extension.outputDirectory)
        }

        target.tasks.configureEach {
            when {
                name.startsWith("assemble") ||
                    name.startsWith("lintVitalAnalyze") ||
                    (name.startsWith("generate") && name.contains("LintVitalReportModel")) -> {
                    dependsOn(downloadTask)
                }
            }
        }

        target.tasks.withType<MergeSourceSetFolders>().configureEach {
            dependsOn(downloadTask)
        }
    }
}
