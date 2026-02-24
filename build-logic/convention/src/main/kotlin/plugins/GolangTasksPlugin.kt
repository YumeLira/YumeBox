package plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.*

class GolangTasksPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.library") {
            target.afterEvaluate {
                configureGolangTasks(target)
            }
        }
    }

    private fun configureGolangTasks(project: Project) {
        val android = project.extensions.findByType(LibraryExtension::class.java)
        if (android == null) {
            project.logger.warn("[GolangTasksPlugin] Android library extension not found in ${project.path}, skipping")
            return
        }
        val golang = project.extensions.findByType(GolangExtension::class.java) ?: return
        val ndkDir = resolveNdkDir(project, android)
        val ndkPath = ndkDir.absolutePath

        val stripTasks = mutableListOf<TaskProvider<Exec>>()
        val copyTasks = mutableListOf<TaskProvider<*>>()

        golang.architectures.get().keys.forEach { abi ->
            val abiBuild = registerBuildTask(project, golang, abi, ndkPath)
            val abiStrip = registerStripTask(project, golang, abi, ndkPath, abiBuild)
            val abiCopy = registerCopyTask(project, golang, abi, abiStrip)
            stripTasks += abiStrip
            copyTasks += abiCopy
        }

        project.tasks.register("buildGolangAll") {
            group = "golang"
            description = "Build Go library for all configured ABIs"
            dependsOn(stripTasks)
        }
        project.tasks.register("copyAllClashLibs") {
            group = "golang"
            description = "Copy Go libraries for all configured ABIs to jniLibs"
            dependsOn(copyTasks)
        }

        project.tasks.register<Delete>("cleanGolangLibs") {
            group = "golang"
            description = "Clean Go libraries"
            delete(project.fileTree("src/jniLibs") { include("**/libclash.so") })
        }
        project.tasks.register<Delete>("cleanGolangCache") {
            group = "golang"
            description = "Clean Go build cache"
            delete(golang.outputDir)
        }
    }

    private fun registerBuildTask(
        project: Project,
        golang: GolangExtension,
        abi: String,
        ndkPath: String,
    ): TaskProvider<Exec> {
        val taskSuffix = GolangUtils.taskSuffixForAbi(abi)
        return project.tasks.register<Exec>("buildGolang$taskSuffix") {
            group = "golang"
            description = "Build Go library for $abi ABI"

            dependsOn("pruneStaleGolangOutputs")

            val sourceDir = golang.sourceDir.get().asFile
            val outputDir = golang.outputDir.get().dir(abi).asFile
            val outputFile = GolangUtils.outputSoFile(outputDir)
            val outputHeader = GolangUtils.outputHeaderFile(outputDir)

            workingDir = sourceDir
            doFirst { outputDir.mkdirs() }
            configureGoBuildCommand(
                task = this,
                golang = golang,
                abi = abi,
                ndkPath = ndkPath,
                outputFile = outputFile,
            )

            inputs.dir(sourceDir)
            inputs.property("abi", abi)
            inputs.property("ndkPath", ndkPath)
            inputs.property("buildTags", golang.buildTags.get())
            inputs.property("buildFlags", golang.buildFlags.orNull ?: GolangExtension.DEFAULT_BUILD_FLAGS)
            inputs.property("postStripStrategy", "llvm-strip-unneeded-v1")
            outputs.file(outputFile)
            outputs.file(outputHeader)
        }
    }

    private fun registerStripTask(
        project: Project,
        golang: GolangExtension,
        abi: String,
        ndkPath: String,
        buildTask: TaskProvider<Exec>,
    ): TaskProvider<Exec> {
        val taskSuffix = GolangUtils.taskSuffixForAbi(abi)
        return project.tasks.register<Exec>("stripGolang$taskSuffix") {
            group = "golang"
            description = "Strip Go shared library for $abi ABI"
            dependsOn(buildTask)

            val outputFile = GolangUtils.outputSoFile(golang.outputDir.get().dir(abi).asFile)
            onlyIf { outputFile.exists() }

            commandLine(
                GolangUtils.getLlvmStripPath(ndkPath),
                "--strip-unneeded",
                outputFile.absolutePath,
            )

            inputs.file(outputFile)
            inputs.property("abi", abi)
            inputs.property("ndkPath", ndkPath)
            inputs.property("postStripStrategy", "llvm-strip-unneeded-v1")
            outputs.file(outputFile)
        }
    }

    private fun configureGoBuildCommand(
        task: Exec,
        golang: GolangExtension,
        abi: String,
        ndkPath: String,
        outputFile: File,
    ) {
        val goArch = golang.architectures.get()[abi] ?: throw GradleException("Unsupported ABI: $abi")
        val clangPath = GolangUtils.getClangPath(ndkPath, abi)
        val linkerFlags = listOf(
            "-Wl,-z,max-page-size=16384",
            "-Wl,-z,common-page-size=16384",
        ).joinToString(" ")

        task.environment("CGO_ENABLED", "1")
        task.environment("GOOS", "android")
        task.environment("GOARCH", goArch)
        if (abi == "armeabi-v7a") {
            task.environment("GOARM", "7")
        }
        task.environment("CC", clangPath)
        task.environment("CXX", clangPath)
        task.environment("CGO_CFLAGS", "-fPIC")
        task.environment("CGO_LDFLAGS", "-fPIC -llog $linkerFlags")

        val buildTags = golang.buildTags.get().joinToString(",")
        val buildFlags = golang.buildFlags.orNull?.takeIf { it.isNotEmpty() } ?: GolangExtension.DEFAULT_BUILD_FLAGS
        val command = buildList {
            add(GolangUtils.getGoBinary())
            add("build")
            add("-buildmode=c-shared")
            if (buildTags.isNotBlank()) {
                add("-tags=$buildTags")
            }
            addAll(buildFlags)
            add("-o")
            add(outputFile.absolutePath)
            add(".")
        }
        task.commandLine(command)
    }

    private fun registerCopyTask(
        project: Project,
        golang: GolangExtension,
        abi: String,
        stripTask: TaskProvider<Exec>,
    ): TaskProvider<org.gradle.api.Task> {
        val taskSuffix = GolangUtils.taskSuffixForAbi(abi)
        return project.tasks.register("copy${taskSuffix}ClashLib") {
            group = "golang"
            description = "Copy Go library for $abi ABI to jniLibs"
            dependsOn(stripTask)

            val outputDir = golang.outputDir.get().dir(abi).asFile
            val sourceFile = GolangUtils.outputSoFile(outputDir)
            val targetDir = project.layout.projectDirectory.dir("src/jniLibs/$abi").asFile
            val targetFile = targetDir.resolve("libclash.so")

            inputs.file(sourceFile)
            outputs.file(targetFile)
            doLast {
                targetDir.mkdirs()
                sourceFile.copyTo(targetFile, overwrite = true)
            }
        }
    }

    private fun resolveNdkDir(project: Project, android: LibraryExtension): File {
        val localProps = loadLocalProperties(project)
        val explicitNdkDir = localProps.getProperty("ndk.dir")?.takeIf { it.isNotBlank() }
        val ndkDir = if (explicitNdkDir != null) {
            File(explicitNdkDir)
        } else {
            val sdkDir = localProps.getProperty("sdk.dir")
                ?: System.getenv("ANDROID_HOME")
                ?: throw GradleException(
                    "Android SDK not found. Please set sdk.dir in local.properties or ANDROID_HOME environment variable.",
                )
            val ndkVersion = android.ndkVersion
                ?: throw GradleException("NDK version not specified in build.gradle")
            File(sdkDir).resolve("ndk").resolve(ndkVersion)
        }

        if (!ndkDir.exists()) {
            throw GradleException(
                "NDK is not installed at '${ndkDir.absolutePath}'. Please install NDK ${android.ndkVersion} via Android SDK Manager.",
            )
        }
        return ndkDir
    }

    private fun loadLocalProperties(project: Project): Properties {
        val localProps = Properties()
        val localPropsFile = project.rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use(localProps::load)
        }
        return localProps
    }
}
