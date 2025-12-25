@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
    id("yumebox.base.android")
    id("yumebox.golang.config")
    id("yumebox.golang.tasks")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.annotation:annotation-jvm:1.9.1")
}

val sixteenKbPageLinkerFlags = listOf("-Wl,-z,max-page-size=16384", "-Wl,-z,common-page-size=16384")
val cmakePageLinkerArgument = "-DYUMEBOX_LINKER_FLAGS:STRING=${sixteenKbPageLinkerFlags.joinToString(" ")}"
val golangSourceDir = file("src/golang/native")
val golangOutputDir = layout.buildDirectory.dir("golang")

val pruneStaleGolangOutputs = tasks.register("pruneStaleGolangOutputs") {
    group = "golang"
    description = "Remove stale golang outputs"
    val outputRoot = golangOutputDir.get().asFile
    val golangAbiFolders = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

    inputs.dir(outputRoot)
        .skipWhenEmpty()

    doLast {
        if (!outputRoot.exists()) return@doLast
        outputRoot.listFiles()
            ?.filter { it.isDirectory && it.name !in golangAbiFolders }
            ?.forEach { stale ->
                stale.deleteRecursively()
                logger.info("Removed stale Golang output directory: ${stale.absolutePath}")
            }
    }
}

abstract class GitCommandValueSource : ValueSource<String, GitCommandValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val workingDir: DirectoryProperty
        val args: ListProperty<String>
    }
    
    @get:Inject
    abstract val execOperations: ExecOperations
    
    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            workingDir = parameters.workingDir.get().asFile
            commandLine(listOf("git") + parameters.args.get())
            standardOutput = output
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return if (result.exitValue == 0) output.toString().trim() else "unknown"
    }
}

val mihomoDir = layout.projectDirectory.dir("src/foss/golang/mihomo")

val gitCommitProvider: Provider<String> = providers.of(GitCommandValueSource::class) {
    parameters {
        workingDir.set(mihomoDir)
        args.set(listOf("rev-parse", "--short", "HEAD"))
    }
}

val gitBranchProvider: Provider<String> = providers.of(GitCommandValueSource::class) {
    parameters {
        workingDir.set(mihomoDir)
        args.set(listOf("branch", "--show-current"))
    }
}

val kernelProps = Properties()
val kernelFile = rootProject.file("kernel.properties")
if (kernelFile.exists()) {
    kernelFile.inputStream().use { kernelProps.load(it) }
}
val mihomoSuffix = kernelProps.getProperty("external.mihomo.suffix", "")!!
val includeTimestamp = kernelProps.getProperty("external.mihomo.includeTimestamp", "false").toBoolean()
val buildTimestampProvider: Provider<String> = providers.provider {
    if (includeTimestamp) SimpleDateFormat("yyMMdd").format(Date()) else ""
}

abstract class MihomoGitInfoTask : DefaultTask() {
    @get:Input
    abstract val gitCommit: Property<String>
    
    @get:Input
    abstract val gitBranch: Property<String>
    
    @get:Input
    abstract val gitSuffix: Property<String>
    
    @get:Input
    abstract val buildTimestamp: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun execute() {
        val versionInfo = mapOf(
            "GIT_COMMIT_HASH" to gitCommit.get(),
            "GIT_BRANCH" to gitBranch.get(),
            "GIT_SUFFIX" to gitSuffix.get(),
            "BUILD_TIMESTAMP" to buildTimestamp.get()
        )
        
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(versionInfo.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
        
        logger.lifecycle("Mihomo Git Info - Commit: ${gitCommit.get()}, Branch: ${gitBranch.get()}, Suffix: '${gitSuffix.get()}', Timestamp: '${buildTimestamp.get()}'")
    }
}

val getMihomoGitInfoTask = tasks.register<MihomoGitInfoTask>("getMihomoGitInfo") {
    group = "build"
    description = "Save mihomo git information for version building"
    
    gitCommit.set(gitCommitProvider)
    gitBranch.set(gitBranchProvider)
    gitSuffix.set(mihomoSuffix)
    buildTimestamp.set(buildTimestampProvider)
    outputFile.set(layout.buildDirectory.file("git-info/mihomo-git-info.txt"))
}

android {
    namespace = gropify.project.namespace.core

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                arguments(
                    "-DGO_SOURCE:STRING=${golangSourceDir.absolutePath}",
                    "-DGO_OUTPUT:STRING=${golangOutputDir.get().asFile.absolutePath}",
                    cmakePageLinkerArgument,
                    "-DGIT_COMMIT_HASH:STRING=${gitCommitProvider.get()}",
                    "-DGIT_BRANCH:STRING=${gitBranchProvider.get()}",
                    "-DGIT_SUFFIX:STRING=${mihomoSuffix}",
                    "-DBUILD_TIMESTAMP:STRING=${buildTimestampProvider.get()}",
                )
            }
        }
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/kotlin")
            jniLibs.srcDirs("src/jniLibs")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

val moduleJvmTarget = gropify.project.jvm.toString()
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(moduleJvmTarget))
}

val abiTaskSuffixes = listOf("arm64v8a", "armeabiv7a", "x86", "x86_64")

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("JniLibFolders")) {
        abiTaskSuffixes.forEach { abi ->
            dependsOn("buildGolang$abi")
            dependsOn("copy${abi}ClashLib")
        }
    }
}

tasks.configureEach {
    if (name.startsWith("buildCMake")) {
        val abi = when {
            name.contains("arm64-v8a") -> "arm64v8a"
            name.contains("armeabi-v7a") -> "armeabiv7a"
            name.contains("x86_64") -> "x86_64"
            name.contains("x86") -> "x86"
            else -> null
        }
        abi?.let {
            dependsOn("buildGolang$it")
            dependsOn("copy${it}ClashLib")
            dependsOn(getMihomoGitInfoTask)
        }
    }
}
