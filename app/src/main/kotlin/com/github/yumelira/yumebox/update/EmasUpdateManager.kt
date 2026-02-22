package com.github.yumelira.yumebox.update

import android.app.Activity
import android.app.Application
import com.taobao.update.IUpdateLog
import com.taobao.update.adapter.UserAction
import com.taobao.update.apk.ApkDownloadListener
import com.taobao.update.apk.ApkUpdater
import com.taobao.update.apk.UpdateResultListener
import com.taobao.update.common.dialog.CustomUpdateInfo
import com.taobao.update.common.dialog.UpdateNotifyListener
import com.taobao.update.common.framework.UpdateRuntime
import com.taobao.update.datasource.UpdateDataSource
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

enum class EmasUpdateDialogType {
    UPDATE_AVAILABLE,
    FORCE_CANCEL,
    INSTALL,
}

data class EmasUpdateDialogEvent(
    val type: EmasUpdateDialogType,
    val title: String,
    val message: String,
    val remoteVersion: String = "",
    val confirmText: String,
    val cancelText: String,
    val forceUpdate: Boolean,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

enum class EmasDownloadStage {
    IDLE,
    PREPARING,
    DOWNLOADING,
    VERIFYING,
    FINISHED,
    ERROR,
}

data class EmasDownloadProgress(
    val stage: EmasDownloadStage = EmasDownloadStage.IDLE,
    val progress: Int = 0,
    val message: String = "",
)

object EmasUpdateDialogBridge {
    private val _event = MutableStateFlow<EmasUpdateDialogEvent?>(null)
    val event: StateFlow<EmasUpdateDialogEvent?> = _event.asStateFlow()
    private val _progress = MutableStateFlow(EmasDownloadProgress())
    val progress: StateFlow<EmasDownloadProgress> = _progress.asStateFlow()

    fun show(event: EmasUpdateDialogEvent) {
        _event.value = event
    }

    fun updateProgress(progress: EmasDownloadProgress) {
        _progress.value = progress
    }

    fun resetProgress() {
        _progress.value = EmasDownloadProgress()
    }

    fun dismiss() {
        _event.value = null
        resetProgress()
    }
}

object EmasUpdateManager {
    private var initialized = false

    fun init(
        application: Application,
        appKey: String,
        appSecret: String,
        channelId: String = "official",
        enableCustomDialog: Boolean = true,
    ) {
        if (initialized) return
        if (appKey.isBlank() || appSecret.isBlank()) {
            Timber.w("EMAS update skipped: appKey/appSecret is empty")
            return
        }

        var step = "init"
        try {
            step = "UpdateDataSource.init"
            UpdateDataSource.getInstance().init(application, appKey, appSecret, channelId)
            step = "UpdateRuntime.init"
            UpdateRuntime.init()
            step = "ApkUpdater.create"
            val apkUpdater = ApkUpdater()
            step = "bindLog"
            bindLog(apkUpdater)
            step = "bindDownloadListener"
            bindDownloadListener(apkUpdater)
            step = "bindResultListener"
            bindResultListener(apkUpdater)
            if (enableCustomDialog) {
                step = "bindDialogListeners"
                bindDialogListeners(apkUpdater)
            } else {
                step = "clearDialogListeners"
                apkUpdater.setUpdateNotifyListener(null)
                apkUpdater.setCancelUpdateNotifyListener(null)
                apkUpdater.setInstallUpdateNotifyListener(null)
            }
            step = "enableCache"
            UpdateDataSource.getInstance().setEnableCache(true)
            step = "setCacheValidTime"
            UpdateDataSource.getInstance().setCacheValidTime(12 * 60 * 60 * 1000L)
            step = "startUpdate"
            UpdateDataSource.getInstance().startUpdate(false)
            initialized = true
            Timber.i("EMAS update initialized")
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "EMAS update init failed at step=$step (invalid argument)")
        } catch (e: IllegalStateException) {
            Timber.e(e, "EMAS update init failed at step=$step (invalid state)")
        } catch (e: SecurityException) {
            Timber.e(e, "EMAS update init failed at step=$step (security)")
        }
    }

    fun startManualUpdate(async: Boolean = true) {
        if (!initialized) {
            Timber.w("EMAS manual update skipped: manager is not initialized")
            return
        }
        try {
            UpdateDataSource.getInstance().startManualUpdate(!async)
        } catch (e: IllegalStateException) {
            Timber.e(e, "EMAS manual update failed (invalid state)")
        } catch (e: SecurityException) {
            Timber.e(e, "EMAS manual update failed (security)")
        }
    }

    private fun bindDialogListeners(apkUpdater: ApkUpdater) {
        apkUpdater.setUpdateNotifyListener(
            createListener(
                type = EmasUpdateDialogType.UPDATE_AVAILABLE,
                title = MLang.Component.Update.Title.Available
            )
        )
        apkUpdater.setCancelUpdateNotifyListener(
            createListener(
                type = EmasUpdateDialogType.FORCE_CANCEL,
                title = MLang.Component.Update.Title.ForceCancel
            )
        )
        apkUpdater.setInstallUpdateNotifyListener(
            createListener(
                type = EmasUpdateDialogType.INSTALL,
                title = MLang.Component.Update.Title.Install
            )
        )
    }

    private fun createListener(
        type: EmasUpdateDialogType,
        title: String,
    ) = UpdateNotifyListener { _: Activity, customUpdateInfo: CustomUpdateInfo, userAction: UserAction ->
        val message = when (type) {
            EmasUpdateDialogType.UPDATE_AVAILABLE -> customUpdateInfo.info.orEmpty()

            EmasUpdateDialogType.FORCE_CANCEL,
            EmasUpdateDialogType.INSTALL,
            -> customUpdateInfo.info.orEmpty()
        }

        EmasUpdateDialogBridge.show(
            EmasUpdateDialogEvent(
                type = type,
                title = title,
                message = message,
                remoteVersion = customUpdateInfo.version.orEmpty(),
                confirmText = userAction.confirmText.orEmpty().ifBlank { MLang.Component.Button.Confirm },
                cancelText = userAction.cancelText.orEmpty().ifBlank { MLang.Component.Button.Cancel },
                forceUpdate = customUpdateInfo.isForceUpdate,
                onConfirm = userAction::onConfirm,
                onCancel = userAction::onCancel,
            )
        )
    }

    private fun bindResultListener(apkUpdater: ApkUpdater) {
        apkUpdater.setUpdateResultListener(UpdateResultListener { mode: Int, errorCode: Int, errMsg: String? ->
            Timber.i("EMAS update result mode=$mode errorCode=$errorCode errMsg=${errMsg.orEmpty()}")
        })
    }

    private fun bindDownloadListener(apkUpdater: ApkUpdater) {
        apkUpdater.setApkDownloadListener(object : ApkDownloadListener {
            override fun onPreDownload() {
                Timber.i("EMAS download start")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = EmasDownloadStage.PREPARING,
                        progress = 0,
                        message = MLang.Component.Update.Message.Preparing,
                    )
                )
            }

            override fun onDownloadProgress(progress: Int) {
                Timber.d("EMAS download progress=$progress")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = EmasDownloadStage.DOWNLOADING,
                        progress = progress.coerceIn(0, 100),
                        message = MLang.Component.Update.Message.DownloadingWithProgress.format(progress),
                    )
                )
            }

            override fun onStartFileMd5Valid(filePath: String?, fileSize: String?) {
                Timber.i("EMAS md5 check start path=$filePath size=$fileSize")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = EmasDownloadStage.VERIFYING,
                        progress = 100,
                        message = MLang.Component.Update.Message.Verifying,
                    )
                )
            }

            override fun onFinishFileMd5Valid(success: Boolean) {
                Timber.i("EMAS md5 check finish success=$success")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = if (success) EmasDownloadStage.FINISHED else EmasDownloadStage.ERROR,
                        progress = 100,
                        message = if (success) {
                            MLang.Component.Update.Message.Finished
                        } else {
                            MLang.Component.Update.Message.VerifyFailed
                        },
                    )
                )
            }

            override fun onDownloadFinish(url: String?, filePath: String?) {
                Timber.i("EMAS download finish url=$url path=$filePath")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = EmasDownloadStage.FINISHED,
                        progress = 100,
                        message = MLang.Component.Update.Message.DownloadReady,
                    )
                )
            }

            override fun onDownloadError(url: String?, errorCode: Int, msg: String?) {
                Timber.e("EMAS download error code=$errorCode url=$url msg=${msg.orEmpty()}")
                EmasUpdateDialogBridge.updateProgress(
                    EmasDownloadProgress(
                        stage = EmasDownloadStage.ERROR,
                        progress = 0,
                        message = MLang.Component.Update.Message.DownloadErrorWithCode.format(errorCode, msg.orEmpty()),
                    )
                )
            }
        })
    }

    private fun bindLog(apkUpdater: ApkUpdater) {
        apkUpdater.setUpdateLog(object : IUpdateLog {
            override fun d(msg: String?) = Timber.d(msg.orEmpty())
            override fun d(msg: String?, throwable: Throwable?) = Timber.d(throwable, msg.orEmpty())
            override fun e(msg: String?) = Timber.e(msg.orEmpty())
            override fun e(msg: String?, throwable: Throwable?) = Timber.e(throwable, msg.orEmpty())
            override fun i(msg: String?) = Timber.i(msg.orEmpty())
            override fun i(msg: String?, throwable: Throwable?) = Timber.i(throwable, msg.orEmpty())
            override fun w(msg: String?) = Timber.w(msg.orEmpty())
            override fun w(msg: String?, throwable: Throwable?) = Timber.w(throwable, msg.orEmpty())
            override fun v(msg: String?) = Timber.v(msg.orEmpty())
            override fun v(msg: String?, throwable: Throwable?) = Timber.v(throwable, msg.orEmpty())
        })
    }
}
