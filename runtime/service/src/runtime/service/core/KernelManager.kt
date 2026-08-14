/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service.core

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Downloadable `libmihomocore.so` catalog and installer. Bundled Alpha remains the fallback. */
object KernelManager {
    const val INDEX_URL =
        "https://github.com/YumeYucca/Kernel-Builder/releases/download/kernel/kernel-index.json"
    const val BUNDLED_ALPHA_ID = "bundled-alpha"
    private const val CORE_FILE_PREFIX = "libmihomocore-"
    private const val CORE_METADATA_SUFFIX = ".json"
    private const val ACTIVE_FILE = "active-kernel"
    private const val ABI = "arm64-v8a"
    private const val SHELL_ABI = 1

    // Kernel-Builder's manifest intentionally carries release, template and toolchain metadata;
    // the runtime validates the fields it consumes while tolerating future metadata additions.
    private val json = Json { ignoreUnknownKeys = true }
    private val installMutex = Mutex()
    // Do not initialize the HTTP engine while the local/bundled core is starting. The network
    // client is only needed after the user opens Kernel settings or selects a remote slot.
    private val client: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(OkHttp) { expectSuccess = false }
    }

    @Serializable
    data class Index(
        val schemaVersion: Int,
        val defaultKernel: String,
        val abi: String,
        val shellAbi: Int,
        val kernels: List<Kernel>,
    )

    @Serializable
    data class Kernel(
        val id: String,
        val name: String,
        val version: String,
        val commit: String,
        val abi: String,
        val shellAbi: Int,
        val downloadUrl: String,
        val sha256: String,
        val sizeBytes: Long,
        val compression: String,
    )

    @Serializable
    private data class InstalledKernel(
        val id: String,
        val name: String,
        val version: String,
        val commit: String,
    )

    suspend fun fetchIndex(url: String = INDEX_URL): Index = withContext(Dispatchers.IO) {
        require(url.startsWith("https://")) { "Kernel index must use HTTPS" }
        val response = client.get(url)
        check(response.status.isSuccess()) { "Kernel index request failed: ${response.status}" }
        val index = json.decodeFromString<Index>(response.bodyAsText())
        validateIndex(index)
        index
    }

    suspend fun install(context: Context, kernel: Kernel): File = installMutex.withLock {
        withContext(Dispatchers.IO) {
            validateKernel(kernel)
            val directory = libraryDirectory(context).apply { mkdirs() }
            val archive = File(directory, "$CORE_FILE_PREFIX${kernel.id}.so.xz.download")
            val expanded = File(directory, "$CORE_FILE_PREFIX${kernel.id}.so.expanded")
            val target = libraryFile(context, kernel.id)
            try {
                download(kernel, archive)
                check(sha256(archive) == kernel.sha256) { "Kernel archive checksum mismatch" }
                XZInputStream(archive.inputStream().buffered()).use { input ->
                    expanded.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                check(expanded.length() > 0L) { "Kernel archive is empty" }
                check(isAndroidArm64Core(expanded)) { "Downloaded file is not a compatible Android ARM64 core" }
                val metadata = metadataFile(context, kernel.id)
                if (metadata.exists()) check(metadata.delete()) { "Unable to replace kernel metadata" }
                replace(target, expanded)
                runCatching { writeInstalledKernel(context, kernel) }.onFailure {
                    Timber.tag("KernelManager").w(it, "Unable to save installed kernel version")
                }
            } finally {
                archive.delete()
                expanded.delete()
            }
            target
        }
    }

    fun libraryFile(context: Context, id: String): File =
        File(libraryDirectory(context), "$CORE_FILE_PREFIX$id.so")

    fun activeKernelId(context: Context): String =
        runCatching {
            File(libraryDirectory(context), ACTIVE_FILE).takeIf(File::isFile)?.readText()?.trim()
        }
            .getOrNull()
            ?.takeIf { it == BUNDLED_ALPHA_ID || (it in setOf("alpha", "meta", "smart") && isInstalled(context, it)) }
            ?: BUNDLED_ALPHA_ID

    fun activate(context: Context, id: String) {
        require(id == BUNDLED_ALPHA_ID || id in setOf("alpha", "meta", "smart"))
        if (id != BUNDLED_ALPHA_ID) {
            check(isAndroidArm64Core(libraryFile(context, id))) { "Kernel is not downloaded or is invalid" }
        }
        val directory = libraryDirectory(context).apply { mkdirs() }
        val marker = File(directory, ACTIVE_FILE)
        val pending = File(directory, "$ACTIVE_FILE.pending")
        pending.writeText(id)
        check(pending.renameTo(marker)) { "Unable to select active kernel" }
    }

    fun installed(context: Context): File? {
        val id = activeKernelId(context)
        return if (id == BUNDLED_ALPHA_ID) null else libraryFile(context, id).takeIf(::isAndroidArm64Core)
    }

    fun isInstalled(context: Context, id: String): Boolean =
        id == BUNDLED_ALPHA_ID || libraryFile(context, id).let(::isAndroidArm64Core)

    /** Returns the source commit tied to the installed file, never the latest remote commit. */
    fun installedCommit(context: Context, id: String): String? {
        if (id == BUNDLED_ALPHA_ID || !isInstalled(context, id)) return null
        readInstalledKernel(context, id)?.let { return it.commit }

        // Kernels installed before metadata support still contain the build-stamped version.
        val commit = readEmbeddedCommit(libraryFile(context, id), id) ?: return null
        runCatching {
            writeInstalledKernel(
                context,
                InstalledKernel(id = id, name = id, version = "$id-$commit", commit = commit),
            )
        }.onFailure {
            Timber.tag("KernelManager").w(it, "Unable to cache embedded kernel version")
        }
        return commit
    }

    private suspend fun download(kernel: Kernel, target: File) {
        target.delete()
        val response = client.get(kernel.downloadUrl)
        check(response.status.isSuccess()) { "Kernel download failed: ${response.status}" }
        response.headers[HttpHeaders.ContentLength]?.toLongOrNull()?.let { length ->
            check(length == kernel.sizeBytes) { "Kernel archive size does not match the index" }
        }
        target.outputStream().buffered().use { output ->
            response.bodyAsChannel().copyTo(output)
        }
        check(target.length() == kernel.sizeBytes) { "Kernel archive size does not match the index" }
    }

    private fun replace(target: File, replacement: File) {
        val backup = File(target.parentFile, "${target.name}.previous")
        backup.delete()
        val hadPrevious = target.exists()
        if (hadPrevious) check(target.renameTo(backup)) { "Unable to back up active kernel" }
        try {
            check(replacement.renameTo(target)) { "Unable to install downloaded kernel" }
            target.setReadable(true, true)
            backup.delete()
        } catch (error: Throwable) {
            if (hadPrevious) backup.renameTo(target)
            throw error
        }
    }

    private fun validateIndex(index: Index) {
        check(index.schemaVersion == 3) { "Unsupported kernel index version" }
        check(index.abi == ABI && index.shellAbi == SHELL_ABI) { "Incompatible kernel index" }
        check(index.kernels.isNotEmpty()) { "Kernel index contains no kernels" }
        check(index.kernels.any { it.id == index.defaultKernel }) { "Invalid default kernel" }
        index.kernels.forEach(::validateKernel)
    }

    private fun validateKernel(kernel: Kernel) {
        check(kernel.id in setOf("alpha", "meta", "smart")) { "Unknown kernel channel" }
        check(kernel.abi == ABI && kernel.shellAbi == SHELL_ABI) { "Incompatible kernel" }
        check(kernel.compression == "xz") { "Unsupported kernel compression" }
        check(kernel.sizeBytes > 0 && kernel.sizeBytes <= 128L * 1024 * 1024) { "Invalid kernel size" }
        check(kernel.downloadUrl.startsWith("https://")) { "Kernel download must use HTTPS" }
        check(kernel.sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid kernel checksum" }
        check(kernel.commit.matches(Regex("[0-9a-f]{40}"))) { "Invalid kernel commit" }
    }

    private fun metadataFile(context: Context, id: String): File =
        File(libraryDirectory(context), "$CORE_FILE_PREFIX$id$CORE_METADATA_SUFFIX")

    private fun readInstalledKernel(context: Context, id: String): InstalledKernel? =
        runCatching {
            json.decodeFromString<InstalledKernel>(metadataFile(context, id).readText())
        }.getOrNull()?.takeIf { metadata ->
            metadata.id == id && metadata.commit.matches(Regex("[0-9a-f]{6,40}"))
        }

    private fun writeInstalledKernel(context: Context, kernel: Kernel) =
        writeInstalledKernel(
            context,
            InstalledKernel(kernel.id, kernel.name, kernel.version, kernel.commit),
        )

    private fun writeInstalledKernel(context: Context, metadata: InstalledKernel) {
        val target = metadataFile(context, metadata.id)
        val pending = File(target.parentFile, "${target.name}.pending")
        pending.writeText(json.encodeToString(metadata))
        if (target.exists()) check(target.delete()) { "Unable to replace kernel metadata" }
        check(pending.renameTo(target)) { "Unable to install kernel metadata" }
    }

    private fun readEmbeddedCommit(file: File, id: String): String? = runCatching {
        val versionPattern =
            when (id) {
                "alpha" -> Regex("(?i)alpha-([0-9a-f]{6,40})")
                "meta" -> Regex("(?i)meta-([0-9a-f]{6,40})")
                "smart" -> Regex("(?i)(?:alpha-smart|smart(?:-smart)?)-([0-9a-f]{6,40})")
                else -> return null
            }
        val buffer = ByteArray(64 * 1024)
        var carry = ""
        file.inputStream().buffered().use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                val chunk = carry + String(buffer, 0, count, StandardCharsets.ISO_8859_1)
                versionPattern.find(chunk)?.groupValues?.get(1)?.let { return it.lowercase() }
                carry = chunk.takeLast(80)
            }
        }
        null
    }.onFailure {
        Timber.tag("KernelManager").w(it, "Unable to read embedded kernel version")
    }.getOrNull()

    private fun isAndroidArm64Core(file: File): Boolean = runCatching {
        if (!file.isFile || file.length() < 64) return false
        val header = ByteArray(64)
        file.inputStream().use { check(it.read(header) == header.size) }
        // ELF64, little-endian, ET_DYN, EM_AARCH64.
        check(header.copyOfRange(0, 4).contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46)))
        check(header[4] == 2.toByte() && header[5] == 1.toByte())
        check(header[16] == 3.toByte() && header[17] == 0.toByte())
        check((header[18].toInt() and 0xff) == 183 && header[19] == 0.toByte())
        file.inputStream().buffered().use { input ->
            val needle = "MihomoMain".toByteArray()
            var matched = 0
            while (true) {
                val value = input.read()
                if (value < 0) break
                matched = if (value.toByte() == needle[matched]) matched + 1 else if (value.toByte() == needle[0]) 1 else 0
                if (matched == needle.size) return true
            }
            false
        }
    }.onFailure { Timber.tag("KernelManager").w(it, "Kernel verification failed") }.getOrDefault(false)

    private fun libraryDirectory(context: Context): File = context.filesDir.resolve("lib")

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").let { digest ->
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
}
