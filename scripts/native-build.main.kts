/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

@file:DependsOn("org.tukaani:xz:1.12")

import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream

class ProjectConfig {
    private val properties = Properties()
    private val kernelFile = File("kernel.properties")
    private val localFile = File("local.properties")
    private val gradleFile = File("gradle.properties")

    init {
        if (kernelFile.exists()) {
            kernelFile.inputStream().use { properties.load(it) }
        }
        if (localFile.exists()) {
            localFile.inputStream().use { properties.load(it) }
        }
        if (gradleFile.exists()) {
            gradleFile.inputStream().use { properties.load(it) }
        }
    }

    fun getString(key: String, default: String = ""): String {
        return System.getProperty(key)
            ?: System.getenv(key.replace('.', '_').uppercase())
            ?: properties.getProperty(key)
            ?: default
    }

    fun getInt(key: String, default: Int): Int {
        return getString(key, default.toString()).toIntOrNull() ?: default
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return getString(key, default.toString()).toBooleanStrictOrNull() ?: default
    }

    fun getCsv(key: String, default: String = ""): List<String> {
        return getString(key, default)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

object SystemDetector {
    val os: String by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "darwin"
            osName.contains("linux") -> "linux"
            else -> "unknown"
        }
    }

    val hostTag: String by lazy {
        val arch = System.getProperty("os.arch")
        when (os) {
            "windows" -> "windows-x86_64"
            "darwin" -> if (arch.contains("aarch64")) "darwin-arm64" else "darwin-x86_64"
            "linux" -> "linux-x86_64"
            else -> "linux-x86_64"
        }
    }

    fun checkCommandExists(cmd: String): Boolean {
        return try {
            val process = if (os == "windows") {
                ProcessBuilder("cmd", "/c", "where", cmd).start()
            } else {
                ProcessBuilder("which", cmd).start()
            }
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

data class CommandResult(
    val success: Boolean,
    val output: String = "",
    val error: String = ""
)

fun executeCommand(
    command: List<String>,
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    printStdout: Boolean = true,
    printStderr: Boolean = true,
    stderrIsError: Boolean = true,
    stdoutPrefix: String? = "[cmd]",
    stderrPrefix: String? = if (stderrIsError) "[err]" else "[cmd]"
): CommandResult {
    return try {
        val processBuilder = ProcessBuilder(command)
        workingDir?.let { processBuilder.directory(it) }
        processBuilder.environment().putAll(environment)

        val process = processBuilder.start()
        val output = StringBuilder()
        val error = StringBuilder()
        val stdoutThread = thread(start = true, name = "stdout-reader") {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    output.appendLine(line)
                    if (printStdout) {
                        if (stdoutPrefix != null) {
                            println("$stdoutPrefix $line")
                        } else {
                            println(line)
                        }
                    }
                }
            }
        }
        val stderrThread = thread(start = true, name = "stderr-reader") {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    error.appendLine(line)
                    if (printStderr) {
                        if (stderrPrefix != null) {
                            println("$stderrPrefix $line")
                        } else {
                            println(line)
                        }
                    }
                }
            }
        }

        val exitCode = process.waitFor()
        stdoutThread.join()
        stderrThread.join()
        CommandResult(
            success = exitCode == 0,
            output = output.toString(),
            error = error.toString()
        )
    } catch (e: Exception) {
        CommandResult(success = false, error = e.message ?: "Unknown error")
    }
}

class NdkTools(private val config: ProjectConfig) {
    private val sdkDir: File by lazy {
        val path = config.getString("sdk.dir", "")
            .takeIf { it.isNotEmpty() }
            ?: System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: throw RuntimeException("Android SDK not found. Please configure sdk.dir or ANDROID_HOME.")
        File(path).also {
            require(it.isDirectory) { "Android SDK not found: ${it.absolutePath}" }
        }
    }

    val ndkDir: File by lazy {
        val explicitNdk = config.getString("ndk.dir", "")
        val ndkVersion = config.getString("android.ndkVersion", "")
        val ndkPath = explicitNdk.takeIf { it.isNotEmpty() }
            ?: File(sdkDir, "ndk/$ndkVersion").absolutePath

        File(ndkPath).also {
            require(it.isDirectory) { "NDK not found: ${it.absolutePath}" }
        }
    }

    fun getClangPath(abi: String): String {
        val triple = when (abi) {
            "arm64-v8a" -> "aarch64-linux-android24"
            "armeabi-v7a" -> "armv7a-linux-androideabi24"
            "x86" -> "i686-linux-android24"
            "x86_64" -> "x86_64-linux-android24"
            else -> throw IllegalArgumentException("Unsupported ABI: $abi")
        }
        val ext = if (SystemDetector.os == "windows") ".cmd" else ""
        return File(ndkDir, "toolchains/llvm/prebuilt/${SystemDetector.hostTag}/bin/${triple}-clang${ext}").absolutePath
    }

    fun getStripPath(): String {
        val ext = if (SystemDetector.os == "windows") ".exe" else ""
        return File(ndkDir, "toolchains/llvm/prebuilt/${SystemDetector.hostTag}/bin/llvm-strip${ext}").absolutePath
    }

    fun getMinAndroidApi(): Int = maxOf(config.getInt("android.minSdk", 24), 24)

    fun getCmakePath(): String {
        val ext = if (SystemDetector.os == "windows") ".exe" else ""
        val cmakeRoot = File(sdkDir, "cmake")
        require(cmakeRoot.isDirectory) { "CMake not found under Android SDK: ${cmakeRoot.absolutePath}" }

        val preferred = listOf("3.22.1")
            .map { File(cmakeRoot, "$it/bin/cmake$ext") }
            .firstOrNull { it.isFile }
        if (preferred != null) {
            return preferred.absolutePath
        }

        return cmakeRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.map { File(it, "bin/cmake$ext") }
            ?.firstOrNull { it.isFile }
            ?.absolutePath
            ?: throw RuntimeException("CMake executable not found under ${cmakeRoot.absolutePath}")
    }

    fun getNinjaPath(): String {
        val ext = if (SystemDetector.os == "windows") ".exe" else ""
        val cmakeRoot = File(sdkDir, "cmake")
        require(cmakeRoot.isDirectory) { "CMake not found under Android SDK: ${cmakeRoot.absolutePath}" }

        val preferred = listOf("3.22.1")
            .map { File(cmakeRoot, "$it/bin/ninja$ext") }
            .firstOrNull { it.isFile }
        if (preferred != null) {
            return preferred.absolutePath
        }

        return cmakeRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.map { File(it, "bin/ninja$ext") }
            ?.firstOrNull { it.isFile }
            ?.absolutePath
            ?: throw RuntimeException("Ninja executable not found under ${cmakeRoot.absolutePath}")
    }
}

class GoBuilder(private val config: ProjectConfig, private val ndkTools: NdkTools) {
    private val sourceDir = File("lib/native/go/native")
    private val outputDir = File("build/native/go")
    private val appJniRoot = File("jniLibs")
    private val goModuleDir = File("lib/native/go")

    private val abiToGoArch = mapOf(
        "arm64-v8a" to "arm64",
        "armeabi-v7a" to "arm",
        "x86" to "386",
        "x86_64" to "amd64"
    )

    private val buildTags = config.getCsv("golang.buildTags", "cmfa")
    private val buildFlags = config.getCsv("golang.buildFlags", "-trimpath")

    fun buildAll() {
        if (!sourceDir.exists()) {
            println("[Go] Source directory not found: ${sourceDir.absolutePath}")
            return
        }

        val abis = config.getCsv("abi.app.list", "armeabi-v7a,arm64-v8a,x86,x86_64")
        println("[Go] Building for ABIs: ${abis.joinToString()}")

        abis.forEach { abi -> buildForAbi(abi) }
    }

    private fun buildForAbi(abi: String) {
        val arch = abiToGoArch[abi] ?: run {
            println("[Go] Unsupported ABI: $abi")
            return
        }

        println("[building] Building for $abi (arch: $arch)...")

        val outputLibDir = File(outputDir, abi)
        outputLibDir.mkdirs()

        val outputFile = File(outputLibDir, "libclash.so")

        val env = buildGoEnv(abi)

        val command = buildList {
            add("go")
            add("build")
            add("-buildmode=c-shared")
            if (buildTags.isNotEmpty()) {
                add("-tags=${buildTags.joinToString(",")}")
            }
            addAll(buildFlags)
            add("-o")
            add(outputFile.absolutePath)
            add(".")
        }

        val result = executeCommand(
            command = command,
            workingDir = sourceDir,
            environment = env,
            stdoutPrefix = "[building][$abi]",
            stderrPrefix = "[building][$abi]",
            stderrIsError = false
        )
        if (result.success) {
            stripLibrary(outputFile)
            copyToAppJni(abi, outputFile)
            println("[building] Successfully built $abi")
        } else {
            val reason = result.error.ifBlank { result.output }.trim()
            println("[building] Failed to build $abi: $reason")
        }
    }

    private fun buildGoEnv(abi: String): Map<String, String> {
        val arch = abiToGoArch.getValue(abi)
        return mapOf(
            "CGO_ENABLED" to "1",
            "GOOS" to "android",
            "GOARCH" to arch,
            "CC" to ndkTools.getClangPath(abi),
            "CXX" to ndkTools.getClangPath(abi),
            "CGO_CFLAGS" to "-fPIC",
            "CGO_LDFLAGS" to "-fPIC -llog -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384",
            "GOWORK" to "off"
        ) + if (abi == "armeabi-v7a") mapOf("GOARM" to "7") else emptyMap()
    }

    private fun stripLibrary(libFile: File) {
        println("[Go] Stripping ${libFile.name}...")
        val command = listOf(ndkTools.getStripPath(), "--strip-unneeded", libFile.absolutePath)
        executeCommand(
            command = command,
            printStdout = false,
            printStderr = false
        )
    }

    private fun copyToAppJni(abi: String, sourceLib: File) {
        val destDir = File(appJniRoot, abi)
        destDir.mkdirs()
        val destLib = File(destDir, "libclash.so")
        sourceLib.copyTo(destLib, overwrite = true)

        val generatedHeader = File(sourceLib.parentFile, "libclash.h")
        if (!generatedHeader.exists()) {
            val fallbackHeader = File(goModuleDir, "libclash.h")
            if (fallbackHeader.exists()) {
                fallbackHeader.copyTo(generatedHeader, overwrite = true)
            }
        }

        println("[Go] Copied to ${destLib.absolutePath}")
    }
}

class RustBuilder(private val config: ProjectConfig) {
    private val sourceDir = File("lib/native/rust")
    private val outputDir = File("build/native/rust")
    private val appJniRoot = File("jniLibs")
    private val outputLibraryName = "libyume_override.so"

    fun buildAll() {
        if (!sourceDir.exists()) {
            println("[Rust] Source directory not found: ${sourceDir.absolutePath}")
            return
        }

        val abis = config.getCsv("abi.app.list", "armeabi-v7a,arm64-v8a,x86,x86_64")
        println("[Rust] Building Android shared library from ${sourceDir.absolutePath}")
        println("[Rust] Host CLI/ELF remains in the rust submodule and is not built by this script")
        println("[Rust] Building for ABIs: ${abis.joinToString()}")

        abis.forEach { abi -> buildForAbi(abi) }
    }

    private fun buildForAbi(abi: String) {
        println("[building] Building for $abi (Rust)...")

        val command = listOf(
            "cargo", "ndk",
            "-t", abi,
            "-o", outputDir.absolutePath,
            "build", "--release", "--lib"
        )

        val result = executeCommand(
            command = command,
            workingDir = sourceDir,
            stdoutPrefix = "[building][$abi]",
            stderrPrefix = "[building][$abi]",
            stderrIsError = false
        )
        if (result.success) {
            val sourceLib = File(outputDir, "$abi/$outputLibraryName")
            if (sourceLib.exists()) {
                copyToAppJni(abi, sourceLib)
                println("[building] Successfully built $abi (Rust)")
            } else {
                println("[building] Output library not found: ${sourceLib.absolutePath}")
            }
        } else {
            val reason = result.error.ifBlank { result.output }.trim()
            println("[building] Failed to build $abi (Rust): $reason")
        }
    }

    private fun copyToAppJni(abi: String, sourceLib: File) {
        val destDir = File(appJniRoot, abi)
        destDir.mkdirs()
        val destLib = File(destDir, "liboverride.so")
        sourceLib.copyTo(destLib, overwrite = true)
        println("[Rust] Copied to ${destLib.absolutePath}")
    }
}

class CppBuilder(private val config: ProjectConfig, private val ndkTools: NdkTools) {
    private val sourceDir = File("lib/native/cpp")
    private val goSourceDir = File("lib/native/go/native")
    private val goOutputDir = File("build/native/go")
    private val gitInfoDir = File("build/native/mihomo")
    private val outputDir = File("build/native/cpp")
    private val appJniRoot = File("jniLibs")

    fun buildAll() {
        if (!sourceDir.exists()) {
            println("[C++] Source directory not found: ${sourceDir.absolutePath}")
            return
        }

        val gitInfoFile = generateGitInfo()
        val abis = config.getCsv("abi.app.list", "armeabi-v7a,arm64-v8a,x86,x86_64")
        println("[C++] Building for ABIs: ${abis.joinToString()}")
        abis.forEach { abi -> buildForAbi(abi, gitInfoFile) }
    }

    fun generateGitInfo(): File {
        val mihomoDir = File("lib/mihomo/mihomo")
        gitInfoDir.mkdirs()

        val gitInfo = mutableMapOf(
            "GIT_COMMIT_HASH" to "unknown",
            "GIT_BRANCH" to "unknown",
            "GIT_SUFFIX" to config.getString("external.mihomo.suffix", ""),
            "BUILD_TIMESTAMP" to ""
        )

        if (mihomoDir.exists()) {
            val commitResult = executeCommand(
                command = listOf("git", "rev-parse", "--short", "HEAD"),
                workingDir = mihomoDir,
                printStdout = false,
                printStderr = false
            )
            if (commitResult.success) {
                gitInfo["GIT_COMMIT_HASH"] = commitResult.output.trim()
            }

            val branchResult = executeCommand(
                command = listOf("git", "branch", "--show-current"),
                workingDir = mihomoDir,
                printStdout = false,
                printStderr = false
            )
            if (branchResult.success) {
                gitInfo["GIT_BRANCH"] = branchResult.output.trim()
            }
        }

        if (config.getBoolean("external.mihomo.includeTimestamp", false)) {
            gitInfo["BUILD_TIMESTAMP"] = SimpleDateFormat("yyMMdd").format(Date())
        }

        val outputFile = File(gitInfoDir, "git-info.txt")
        outputFile.writeText(
            gitInfo.entries.joinToString("\n") { "${it.key}=${it.value}" }
        )
        println("[CMake] Generated git-info.txt: ${outputFile.absolutePath}")
        return outputFile
    }

    private fun buildForAbi(abi: String, gitInfoFile: File) {
        val goLibDir = File(goOutputDir, abi)
        val goHeader = File(goLibDir, "libclash.h")
        val goLibrary = File(goLibDir, "libclash.so")
        if (!goHeader.exists() || !goLibrary.exists()) {
            println("[building][$abi] Skipping: Go outputs missing at ${goLibDir.absolutePath}")
            return
        }

        println("[building] Building for $abi (C++)...")
        val buildDir = File(outputDir, abi)
        val objDir = File(outputDir, "obj/$abi")
        buildDir.mkdirs()
        objDir.mkdirs()

        val cmakePath = ndkTools.getCmakePath()
        val ninjaPath = ndkTools.getNinjaPath()
        val apiLevel = ndkTools.getMinAndroidApi()

        val configureCommand = listOf(
            cmakePath,
            "-S", sourceDir.absolutePath,
            "-B", buildDir.absolutePath,
            "-G", "Ninja",
            "-DCMAKE_SYSTEM_NAME=Android",
            "-DCMAKE_SYSTEM_VERSION=$apiLevel",
            "-DANDROID_PLATFORM=android-$apiLevel",
            "-DANDROID_ABI=$abi",
            "-DCMAKE_ANDROID_ARCH_ABI=$abi",
            "-DANDROID_NDK=${ndkTools.ndkDir.absolutePath}",
            "-DCMAKE_ANDROID_NDK=${ndkTools.ndkDir.absolutePath}",
            "-DCMAKE_TOOLCHAIN_FILE=${File(ndkTools.ndkDir, "build/cmake/android.toolchain.cmake").absolutePath}",
            "-DCMAKE_MAKE_PROGRAM=$ninjaPath",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${objDir.absolutePath}",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${objDir.absolutePath}",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DGO_SOURCE:STRING=${goSourceDir.absolutePath}",
            "-DGO_OUTPUT:STRING=${goOutputDir.absolutePath}",
            "-DYUMEBOX_LINKER_FLAGS:STRING=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384",
            "-DGIT_INFO_FILE:STRING=${gitInfoFile.absolutePath}"
        )
        val configureResult = executeCommand(
            command = configureCommand,
            stdoutPrefix = "[building][$abi][cmake]",
            stderrPrefix = "[building][$abi][cmake]",
            stderrIsError = false
        )
        if (!configureResult.success) {
            val reason = configureResult.error.ifBlank { configureResult.output }.trim()
            println("[building][$abi] Failed to configure: $reason")
            return
        }

        val buildResult = executeCommand(
            command = listOf(cmakePath, "--build", buildDir.absolutePath, "--target", "bridge"),
            stdoutPrefix = "[building][$abi][cmake]",
            stderrPrefix = "[building][$abi][cmake]",
            stderrIsError = false
        )
        if (!buildResult.success) {
            val reason = buildResult.error.ifBlank { buildResult.output }.trim()
            println("[building][$abi] Failed to build: $reason")
            return
        }

        val builtLib = File(objDir, "libbridge.so")
        if (!builtLib.exists()) {
            println("[building][$abi] Output library not found: ${builtLib.absolutePath}")
            return
        }

        stripLibrary(builtLib)
        copyToAppJni(abi, builtLib)
        println("[building][$abi] Successfully built (C++)")
    }

    private fun stripLibrary(libFile: File) {
        executeCommand(
            command = listOf(ndkTools.getStripPath(), "--strip-unneeded", libFile.absolutePath),
            printStdout = false,
            printStderr = false
        )
    }

    private fun copyToAppJni(abi: String, sourceLib: File) {
        val destDir = File(appJniRoot, abi)
        destDir.mkdirs()
        val destLib = File(destDir, "libbridge.so")
        sourceLib.copyTo(destLib, overwrite = true)
        println("[C++] Copied to ${destLib.absolutePath}")
    }
}

// ==================== resource-downloader.kts ====================

class ResourceDownloader(private val config: ProjectConfig) {
    private val outputDir = File("build/generated/assets/geo")

    fun downloadGeoFiles() {
        outputDir.mkdirs()

        val assets = listOf(
            AssetInfo("geoip.metadb", config.getString("asset.geoip.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb")),
            AssetInfo("geosite.dat", config.getString("asset.geosite.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat")),
            AssetInfo("ASN.mmdb", config.getString("asset.asn.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb"))
        )

        assets.forEach { asset ->
            if (asset.url.isNotEmpty() && asset.url.startsWith("https://")) {
                downloadFile(asset.name, asset.url, compress = true)
            }
        }
    }

    private data class AssetInfo(val name: String, val url: String)

    private fun downloadFile(name: String, url: String, compress: Boolean = false) {
        try {
            println("[Geo] Downloading $name from $url...")

            val tempFile = File.createTempFile("geo-", "-${name}")
            tempFile.deleteOnExit()

            val connection = URL(url).openConnection()
            connection.connect()
            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (compress) {
                val outputFile = File(outputDir, "${name}.xz")
                compressToXz(tempFile, outputFile)
                println("[Geo] Downloaded and compressed $name -> ${outputFile.absolutePath}")
            } else {
                val outputFile = File(outputDir, name)
                tempFile.copyTo(outputFile, overwrite = true)
                println("[Geo] Downloaded $name to ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("[Geo] Failed to download $name: ${e.message}")
        }
    }

    private fun compressToXz(sourceFile: File, outputFile: File) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        sourceFile.inputStream().buffered().use { input ->
            outputFile.outputStream().buffered().use { fileOutput ->
                XZOutputStream(fileOutput, LZMA2Options()).use { xzOutput ->
                    input.copyTo(xzOutput)
                }
            }
        }
    }
}

fun printUsage() {
    println("""
        YumeBox Native Build Tool

        Usage: kotlin scripts/native-build.main.kts [options]

        Options:
          --go       Build Go native libraries
          --rust     Build Rust config compiler
          --cpp      Generate CMake/git info
          --geo      Download and compress GeoIP/GeoSite assets into app/assets with XZ
          --clean    Clean build outputs
          --all      Build everything (default)
          --help     Show this help
    """.trimIndent())
}

fun cleanBuildOutputs() {
    println("[Clean] Removing build outputs...")
    File("build/native").deleteRecursively()
    File("build/generated").deleteRecursively()
    listOf("geoip.metadb.xz", "geosite.dat.xz", "ASN.mmdb.xz").forEach { name ->
        File("app/assets/$name").delete()
    }

    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    abis.forEach { abi ->
        File("jniLibs/$abi/libclash.so").delete()
        File("jniLibs/$abi/liboverride.so").delete()
        File("jniLibs/$abi/libbridge.so").delete()
    }
    println("[Clean] Done")
}

val message = """
 __   __                             ____                 
 \ \ / /  _   _   _ __ ___     ___  | __ )    ___   __  __
  \ V /  | | | | | '_ ` _ \   / _ \ |  _ \   / _ \  \ \/ /
   | |   | |_| | | | | | | | |  __/ | |_) | | (_) |  >  < 
   |_|    \__,_| |_| |_| |_|  \___| |____/   \___/  /_/\_\
                                                          
""".trimIndent()


fun main(args: Array<String>) {
    if (args.contains("--help")) {
        printUsage()
        return
    }

    println(message)
    println("=== YumeBox Native Build Tool ===")
    println("OS: ${SystemDetector.os}, Host: ${SystemDetector.hostTag}")

    if (args.contains("--clean")) {
        cleanBuildOutputs()
        return
    }

    val config = ProjectConfig()
    val ndkTools = NdkTools(config)

    println("NDK: ${ndkTools.ndkDir.absolutePath}")
    println("Go: ${if (SystemDetector.checkCommandExists("go")) "OK" else "NOT FOUND"}")
    println("Rust: ${if (SystemDetector.checkCommandExists("cargo")) "OK" else "NOT FOUND"}")
    println("XZ library: org.tukaani:xz:1.12")

    val buildGo = args.isEmpty() || args.contains("--all") || args.contains("--go")
    val buildRust = args.isEmpty() || args.contains("--all") || args.contains("--rust")
    val buildCpp = args.isEmpty() || args.contains("--all") || args.contains("--cpp")
    val downloadGeo = args.isEmpty() || args.contains("--all") || args.contains("--geo")

    if (buildGo) {
        GoBuilder(config, ndkTools).buildAll()
    }

    if (buildRust) {
        RustBuilder(config).buildAll()
    }

    if (buildCpp) {
        CppBuilder(config, ndkTools).buildAll()
    }

    if (downloadGeo) {
        ResourceDownloader(config).downloadGeoFiles()
    }

    println("=== Build Complete ===")
}

main(args)
