package plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File

class GolangTasksPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.library") {
            target.afterEvaluate {
                configureTasks(target)
            }
        }
    }

    private fun configureTasks(target: Project) {
        val golang = target.extensions.findByType(GolangExtension::class.java) ?: return

        // Get Android SDK directory
        val localProperties = target.rootProject.file("local.properties")
        val sdkDir = if (localProperties.exists()) {
            val props = java.util.Properties()
            props.load(localProperties.inputStream())
            target.file(
                props.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
                ?: throw GradleException("Android SDK not found")
            )
        } else {
            target.file(
                System.getenv("ANDROID_HOME") ?: throw GradleException("Android SDK not found")
            )
        }

        val ndkDir = sdkDir.resolve("ndk").listFiles()?.maxByOrNull { it.name }
            ?: throw GradleException("NDK is not installed. Please install NDK via Android SDK Manager.")

        if (!ndkDir.exists()) {
            throw GradleException("NDK is not installed at '${ndkDir.absolutePath}'. Please install NDK via Android SDK Manager.")
        }
        val ndkPath = ndkDir.absolutePath

        val abis = golang.architectures.get().keys
        abis.forEach { abi ->
            val abiNormalized = abi.replace("-", "")
            val buildTask = target.tasks.register<Exec>("buildGolang${abiNormalized}") {
                group = "golang"
                description = "Build Go library for $abi ABI"

                dependsOn("pruneStaleGolangOutputs")

                val sourceDir = golang.sourceDir.get().asFile
                val outputDir = golang.outputDir.get().dir(abi).asFile
                val outputFile = target.file(outputDir).resolve("libclash.so")

                doFirst {
                    outputDir.mkdirs()
                }

                workingDir = sourceDir

                doFirst {
                    val goArch = golang.architectures.get()[abi]
                        ?: throw GradleException("Unsupported ABI: $abi")
                    val clangPath = GolangUtils.getClangPath(ndkPath, abi)

                    val sixteenKbPageLinkerFlags =
                        listOf("-Wl,-z,max-page-size=16384", "-Wl,-z,common-page-size=16384")
                    val linkerFlags = sixteenKbPageLinkerFlags.joinToString(" ")

                    environment("CGO_ENABLED", "1")
                    environment("GOOS", "android")
                    environment("GOARCH", goArch)
                    if (abi == "armeabi-v7a") environment("GOARM", "7")
                    environment("CC", clangPath)
                    environment("CXX", clangPath)
                    environment("CGO_CFLAGS", "-fPIC")
                    environment("CGO_LDFLAGS", "-fPIC -llog $linkerFlags")

                    val buildTags = golang.buildTags.get().joinToString(",")
                    val buildFlags = golang.buildFlags.orNull?.takeIf { it.isNotEmpty() }
                        ?: GolangExtension.DEFAULT_BUILD_FLAGS

                    val command = mutableListOf(
                        GolangUtils.getGoBinary(),
                        "build",
                        "-buildmode=c-shared",
                    )
                    if (buildTags.isNotBlank()) {
                        command += "-tags=$buildTags"
                    }
                    command.addAll(buildFlags)
                    command += listOf("-o", outputFile.absolutePath, ".")
                    commandLine = command
                }

                inputs.dir(sourceDir)
                inputs.property("abi", abi)
                inputs.property("ndkPath", ndkPath)
                inputs.property("buildTags", golang.buildTags.get())
                inputs.property(
                    "buildFlags",
                    golang.buildFlags.orNull ?: GolangExtension.DEFAULT_BUILD_FLAGS
                )
                outputs.file(outputFile)
                outputs.file(target.file(outputDir).resolve("libclash.h"))
            }
            target.tasks.register("copy${abiNormalized}ClashLib") {
                group = "golang"
                description = "Copy Go library for $abi ABI to jniLibs"
                dependsOn(buildTask)
                val outputDir = golang.outputDir.get().dir(abi).asFile
                val sourceFile = File(outputDir, "libclash.so")
                val targetDir = target.layout.projectDirectory.dir("src/jniLibs/$abi").asFile
                val targetFile = targetDir.resolve("libclash.so")
                inputs.file(sourceFile)
                outputs.file(targetFile)
                doLast {
                    targetDir.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
            }
        }
        target.tasks.register<Delete>("cleanGolangLibs") {
            group = "golang"
            description = "Clean Go libraries"
            delete(target.fileTree("src/jniLibs") {
                include("**/libclash.so")
            })
        }
        target.tasks.register<Delete>("cleanGolangCache") {
            group = "golang"
            description = "Clean Go build cache"
            delete(golang.outputDir)
        }
    }
}