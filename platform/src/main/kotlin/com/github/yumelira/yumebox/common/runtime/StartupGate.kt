package com.github.yumelira.yumebox.common.runtime

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.android.apksig.ApkVerifier
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.zip.ZipFile
import kotlin.system.exitProcess

object StartupGate {
    private const val maskBase = 0x39

    @Volatile
    private var primaryLoaded = false

    fun verify(application: Application) {
        val isDebuggable =
            (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        runCatching {
            val ctx = application.applicationContext
            if (!checkPkg(ctx.packageName)) die()
            if (!checkApkPath(ctx.packageName, application.applicationInfo.sourceDir)) die()
            if (!checkApkV2(application.applicationInfo.sourceDir)) die()
            if (!checkSigner(application.packageManager, ctx.packageName)) die()
            if (!checkAppClass(ctx::class.java.name)) die()
            if (!checkAppParent(ctx::class.java.superclass?.name)) die()
            if (!checkPackagedPrimary(application)) die()
        }.getOrElse { throwable ->
            if (isDebuggable) {
                Timber.e(throwable, "startup gate failed")
            }
            die()
        }
    }

    fun loadPrimary() {
        if (primaryLoaded) return
        synchronized(this) {
            if (primaryLoaded) return
            System.loadLibrary(unmask(intArrayOf(64, 79, 86, 89)))
            primaryLoaded = true
        }
    }

    private fun checkPkg(actual: String): Boolean = actual == unmask(
        intArrayOf(
            90, 85, 86, 18, 90, 87, 75, 40, 52, 32, 109, 61, 48,
            84, 95, 87, 85, 79, 95, 17, 57, 52, 47, 38, 38, 42, 65,
        )
    )

    private fun checkApkPath(packageName: String, sourceDir: String?): Boolean {
        if (sourceDir.isNullOrBlank()) return false
        val pmPath = queryPmPath(packageName) ?: return false
        return sourceDir == pmPath
    }

    private fun checkApkV2(sourceDir: String?): Boolean {
        if (sourceDir.isNullOrBlank()) return false
        return runCatching {
            ApkVerifier.Builder(File(sourceDir)).build().verify().isVerifiedUsingV2Scheme
        }.getOrDefault(false)
    }

    private fun checkSigner(pm: PackageManager, packageName: String): Boolean {
        val digests = getSignerSha256(pm, packageName)
        if (digests.isEmpty()) return false
        val isDebuggable = isDebuggablePackage(pm, packageName)
        if (isDebuggable) return true
        return digests.any { it == releaseFingerprint() }
    }

    private fun isDebuggablePackage(pm: PackageManager, packageName: String): Boolean {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        return (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun getSignerSha256(pm: PackageManager, packageName: String): List<String> {
        val pkg = runCatching { getPackageInfoCompat(pm, packageName) }.getOrNull() ?: return emptyList()
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkg.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            pkg.signatures?.map { it.toByteArray() }.orEmpty()
        }
        if (signatures.isEmpty()) return emptyList()

        val md = MessageDigest.getInstance(unmask(intArrayOf(106, 114, 122, 17, 15, 11, 9)))
        val certFactory = CertificateFactory.getInstance("X.509")
        return signatures.map { raw ->
            val cert = certFactory.generateCertificate(ByteArrayInputStream(raw)) as X509Certificate
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { byte -> "%02X".format(Locale.US, byte.toInt() and 0xFF) }
        }
    }

    private fun getPackageInfoCompat(pm: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
    }

    private fun checkAppClass(actual: String?): Boolean = actual == unmask(
        intArrayOf(
            90, 85, 86, 18, 90, 87, 75, 40, 52, 32, 109, 61, 48,
            84, 95, 87, 85, 79, 95, 17, 57, 52, 47, 38, 38, 42, 65,
            20, 122, 76, 77,
        )
    )

    private fun checkAppParent(actual: String?): Boolean = actual == unmask(
        intArrayOf(
            88, 84, 95, 78, 82, 87, 91, 110, 32, 50, 51, 106, 4,
            73, 74, 87, 85, 94, 95, 75, 41, 46, 44,
        )
    )

    private fun checkPackagedPrimary(application: Application): Boolean {
        val soName = System.mapLibraryName(unmask(intArrayOf(64, 79, 86, 89)))
        val apkPaths = buildList {
            add(application.applicationInfo.sourceDir)
            application.applicationInfo.splitSourceDirs?.let(::addAll)
        }.filter { !it.isNullOrBlank() }

        return apkPaths.any { apkPath ->
            runCatching {
                ZipFile(apkPath).use { zip ->
                    zip.entries().asSequence().any { entry ->
                        !entry.isDirectory &&
                            entry.name.startsWith("lib/") &&
                            entry.name.endsWith("/$soName")
                    }
                }
            }.getOrDefault(false)
        }
    }

    private fun queryPmPath(packageName: String): String? = runCatching {
        val process = ProcessBuilder("sh", "-c", "pm path $packageName")
            .redirectErrorStream(true)
            .start()
        try {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("package:") }?.removePrefix("package:")
            }
        } finally {
            process.destroy()
        }
    }.getOrNull()

    private fun releaseFingerprint(): String = unmask(
        intArrayOf(
            11, 123, 1, 122, 5, 4, 12, 6, 123, 4, 6, 126, 114,
            10, 0, 121, 120, 7, 13, 124, 122, 114, 118, 121, 117, 116,
            3, 9, 122, 6, 121, 13, 5, 118, 114, 120, 115, 119, 127, 122,
            15, 1, 121, 4, 4, 7, 1, 123, 7, 123, 126, 124, 0, 0,
            14, 11, 7, 7, 8, 122, 119, 122, 121, 112, 112, 3, 2, 3,
            6, 5, 15, 5, 1, 121, 120, 0, 125, 127, 14, 10, 1, 10,
            15, 4, 8, 2, 123, 115, 122, 126, 0, 13, 0, 2, 126,
        )
    )

    private fun unmask(data: IntArray): String = buildString(data.size) {
        data.forEachIndexed { index, value ->
            append((value xor (maskBase + (index % 13))).toChar())
        }
    }

    private fun die(): Nothing {
        Process.killProcess(Process.myPid())
        exitProcess(-1)
    }
}
