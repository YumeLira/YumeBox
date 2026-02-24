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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plugins

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import java.io.File

abstract class GolangExtension {
    abstract val sourceDir: DirectoryProperty
    abstract val outputDir: DirectoryProperty
    abstract val architectures: MapProperty<String, String>
    abstract val buildTags: ListProperty<String>
    abstract val buildFlags: ListProperty<String>

    companion object {
        val DEFAULT_ARCHITECTURES = mapOf(
            "armeabi-v7a" to "arm",
            "arm64-v8a" to "arm64",
            "x86" to "386",
            "x86_64" to "amd64",
        )
        val DEFAULT_BUILD_TAGS = listOf("with_gvisor", "cmfa")
        val DEFAULT_BUILD_FLAGS = listOf("-v", "-trimpath", "-ldflags=-s -w -buildid=")
    }
}

object GolangUtils {
    private fun ndkHostTag(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("windows") -> "windows-x86_64"
            osName.contains("mac") || osName.contains("darwin") -> "darwin-x86_64"
            osName.contains("linux") -> "linux-x86_64"
            else -> error("Unsupported OS: $osName")
        }
    }

    fun getGoBinary(): String = System.getenv("GO_EXECUTABLE") ?: "go"

    fun getLlvmStripPath(ndkDir: String): String {
        val host = ndkHostTag()
        return "$ndkDir/toolchains/llvm/prebuilt/$host/bin/llvm-strip"
    }

    fun getClangPath(ndkDir: String, abi: String): String {
        val host = ndkHostTag()
        val prefix = clangPrefixForAbi(abi)
        return "$ndkDir/toolchains/llvm/prebuilt/$host/bin/$prefix"
    }

    fun taskSuffixForAbi(abi: String): String = abi.replace("-", "")

    fun outputSoFile(outputDir: File): File = outputDir.resolve("libclash.so")

    fun outputHeaderFile(outputDir: File): File = outputDir.resolve("libclash.h")

    private fun clangPrefixForAbi(abi: String): String = when (abi) {
        "armeabi-v7a" -> "armv7a-linux-androideabi21-clang"
        "arm64-v8a" -> "aarch64-linux-android21-clang"
        "x86" -> "i686-linux-android21-clang"
        "x86_64" -> "x86_64-linux-android21-clang"
        else -> error("Unsupported ABI: $abi")
    }
}
