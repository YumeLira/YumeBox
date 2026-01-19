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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.common.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveUtil {

    fun unzipZip(
        zipFile: File,
        destination: File
    ): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) return false

        return runCatching {
            prepareDestination(destination)

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }.forEach { entry ->
                    val outFile = resolveEntryTarget(destination, entry.name)

                    if (entry.isDirectory) {
                        ensureDirectory(outFile)
                    } else {
                        writeEntry(zis, outFile)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun addToZip(
        file: File,
        path: String,
        zos: ZipOutputStream,
        onProgress: ((String, Long) -> Unit)? = null,
    ) {
        runCatching {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    addToZip(child, "$path/${child.name}", zos, onProgress)
                }
            } else {
                val entry = ZipEntry(path)
                entry.time = file.lastModified()
                zos.putNextEntry(entry)

                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }

                zos.closeEntry()
                onProgress?.invoke(path, file.length())
            }
        }
    }

    fun untar(
        tarFile: File,
        destination: File
    ): Boolean {
        if (!tarFile.exists() || !tarFile.isFile) return false

        return runCatching {
            prepareDestination(destination)

            TarArchiveInputStream(FileInputStream(tarFile)).use { tis ->
                var entry: TarArchiveEntry? = tis.nextTarEntry
                while (entry != null) {
                    val outFile = resolveEntryTarget(destination, entry.name)

                    if (entry.isDirectory) {
                        ensureDirectory(outFile)
                    } else {
                        writeEntry(tis, outFile)
                    }

                    entry = tis.nextTarEntry
                }
            }
            true
        }.getOrDefault(false)
    }

    fun untarGz(
        tarGzFile: File,
        destination: File
    ): Boolean {
        if (!tarGzFile.exists() || !tarGzFile.isFile) return false

        return runCatching {
            prepareDestination(destination)

            FileInputStream(tarGzFile).use { fis ->
                GzipCompressorInputStream(fis).use { gzip ->
                    TarArchiveInputStream(gzip).use { tis ->
                        var entry: TarArchiveEntry? = tis.nextTarEntry
                        while (entry != null) {
                            val outFile = resolveEntryTarget(destination, entry.name)

                            if (entry.isDirectory) {
                                ensureDirectory(outFile)
                            } else {
                                writeEntry(tis, outFile)
                            }

                            entry = tis.nextTarEntry
                        }
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun prepareDestination(destination: File) {
        if (!destination.exists()) {
            if (!destination.mkdirs()) throw IllegalStateException("无法创建目录: ${destination.absolutePath}")
        }
        if (!destination.isDirectory) throw IllegalStateException("目标不是目录: ${destination.absolutePath}")
    }

    private fun resolveEntryTarget(destination: File, entryName: String): File {
        val targetFile = File(destination, entryName)
        val canonicalDestination = destination.canonicalFile
        val canonicalTarget = targetFile.canonicalFile

        val relativePath = getRelativePath(canonicalDestination, canonicalTarget)
        if (relativePath == null || relativePath.startsWith("..")) {
            throw SecurityException("检测到路径遍历: $entryName")
        }
        return targetFile
    }

    private fun getRelativePath(base: File, target: File): String? {
        val basePath = base.absolutePath
        val targetPath = target.absolutePath

        return if (targetPath.startsWith(basePath)) {
            if (basePath == targetPath) "" else targetPath.substring(basePath.length + 1)
        } else {
            null
        }
    }

    private fun ensureDirectory(directory: File) {
        if (directory.exists()) {
            if (!directory.isDirectory) throw IllegalStateException("路径已存在但不是目录: ${directory.absolutePath}")
        } else if (!directory.mkdirs()) {
            throw IllegalStateException("无法创建目录: ${directory.absolutePath}")
        }
    }

    private fun writeEntry(input: InputStream, targetFile: File) {
        targetFile.parentFile?.let { ensureDirectory(it) }
        FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
        }
    }
}