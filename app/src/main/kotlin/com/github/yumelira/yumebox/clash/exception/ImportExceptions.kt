package com.github.yumelira.yumebox.clash.exception

sealed class ConfigImportException(
    message: String, cause: Throwable? = null
) : Exception(message, cause) {
    abstract val isRetryable: Boolean

    abstract val userFriendlyMessage: String
}

class NetworkException(
    message: String, cause: Throwable? = null, val url: String? = null, val statusCode: Int? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = true
    override val userFriendlyMessage: String
        get() = when {
            statusCode != null -> "网络请求失败 (HTTP $statusCode): $message"
            url != null -> "无法连接到服务器: $url"
            else -> "网络连接失败: $message"
        }
}

class ConfigValidationException(
    message: String, cause: Throwable? = null, val validationErrors: List<String> = emptyList()
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = if (validationErrors.isNotEmpty()) {
            "配置文件格式错误:\n${validationErrors.joinToString("\n")}"
        } else {
            "配置文件格式错误: $message"
        }
}

class FileAccessException(
    message: String, val reason: Reason = Reason.UNKNOWN, cause: Throwable? = null, val filePath: String? = null
) : ConfigImportException(message, cause) {

    enum class Reason {
        NOT_FOUND, NO_READ_PERMISSION, NO_WRITE_PERMISSION, INSUFFICIENT_SPACE, INVALID_PATH, UNKNOWN
    }

    override val isRetryable: Boolean = reason == Reason.INSUFFICIENT_SPACE
    override val userFriendlyMessage: String
        get() = when (reason) {
            Reason.NOT_FOUND -> "文件不存在: ${filePath ?: "未知路径"}"
            Reason.NO_READ_PERMISSION -> "没有读取权限: ${filePath ?: "未知路径"}"
            Reason.NO_WRITE_PERMISSION -> "没有写入权限: ${filePath ?: "未知路径"}"
            Reason.INSUFFICIENT_SPACE -> "存储空间不足"
            Reason.INVALID_PATH -> "无效的文件路径: ${filePath ?: "未知路径"}"
            Reason.UNKNOWN -> "文件访问错误: $message"
        }
}


class TimeoutException(
    message: String, cause: Throwable? = null, val timeoutMs: Long? = null, val operation: String? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = true
    override val userFriendlyMessage: String
        get() {
            val opDesc = operation?.let { "[$it]" } ?: ""
            val timeDesc = timeoutMs?.let { " (超时时间: ${it / 1000}秒)" } ?: ""
            return "操作超时$opDesc$timeDesc"
        }
}


class ImportCancelledException(
    message: String = "导入已被取消"
) : ConfigImportException(message, null) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = message ?: "导入已被取消"
}

class UnknownException(
    message: String, cause: Throwable? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = "$message"
}

fun Throwable.toConfigImportException(): ConfigImportException {
    return when (this) {
        is ConfigImportException -> this

        is com.github.yumelira.yumebox.core.bridge.ClashException -> ConfigValidationException(
            this.message ?: "配置验证失败", this
        )

        is java.net.UnknownHostException -> NetworkException(
            "无法解析域名: ${this.message}", this
        )

        is java.net.SocketTimeoutException -> TimeoutException(
            "网络请求超时", this, operation = "网络请求"
        )

        is java.net.ConnectException -> NetworkException(
            "无法连接到服务器: ${this.message}", this
        )

        is java.io.FileNotFoundException -> FileAccessException(
            this.message ?: "文件不存在", FileAccessException.Reason.NOT_FOUND, this
        )

        is java.io.IOException -> when {
            message?.contains("Permission denied", ignoreCase = true) == true -> FileAccessException(
                this.message ?: "权限被拒绝", FileAccessException.Reason.NO_WRITE_PERMISSION, this
            )

            message?.contains("No space left", ignoreCase = true) == true -> FileAccessException(
                "存储空间不足", FileAccessException.Reason.INSUFFICIENT_SPACE, this
            )

            else -> FileAccessException(
                this.message ?: "文件操作失败", FileAccessException.Reason.UNKNOWN, this
            )
        }

        is kotlinx.coroutines.CancellationException -> ImportCancelledException()
        is java.util.concurrent.TimeoutException -> TimeoutException(
            "操作超时", this
        )

        else -> UnknownException(this.message ?: "未知错误", this)
    }
}