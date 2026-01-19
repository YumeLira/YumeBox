package com.github.yumelira.yumebox.clash.importer

sealed class ImportState {
    object Idle : ImportState()

    data class Preparing(
        val message: String = "准备导入..."
    ) : ImportState()

    data class Downloading(
        val progress: Int,
        val currentBytes: Long,
        val totalBytes: Long,
        val message: String = "下载配置中..."
    ) : ImportState() {
        val progressPercent: Int
            get() = if (totalBytes > 0) {
                ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            } else progress
    }

    data class Copying(
        val progress: Int,
        val message: String = "复制文件中..."
    ) : ImportState()

    data class Validating(
        val progress: Int,
        val message: String = "验证配置..."
    ) : ImportState()

    data class LoadingProviders(
        val progress: Int,
        val currentProvider: String?,
        val totalProviders: Int,
        val completedProviders: Int,
        val message: String = "加载资源..."
    ) : ImportState() {
        val providerProgress: String
            get() = "$completedProviders/$totalProviders"
    }

    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
        val reason: String,
        val nextRetryInSeconds: Int
    ) : ImportState() {
        val retryMessage: String
            get() = "第 $attempt/$maxAttempts 次重试，原因: $reason"
    }

    data class Cleaning(
        val message: String = "清理临时文件..."
    ) : ImportState()

    data class Success(
        val configPath: String,
        val message: String = "导入成功"
    ) : ImportState()

    data class Failed(
        val error: String,
        val exception: Throwable?,
        val isRetryable: Boolean = false
    ) : ImportState() {
        val shortError: String
            get() = error.take(200)
    }

    data class Cancelled(
        val message: String = "导入已取消"
    ) : ImportState()

}

