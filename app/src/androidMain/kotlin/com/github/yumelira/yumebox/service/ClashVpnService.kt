package com.github.yumelira.yumebox.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.github.yumelira.yumebox.clash.config.Configuration
import com.github.yumelira.yumebox.clash.config.RouteConfig
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.net.InetSocketAddress
import java.security.SecureRandom

class ClashVpnService : VpnService() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_PROFILE_ID = "profile_id"

        private val random = SecureRandom()

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ClashVpnService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }

    private val clashManager: ClashManager by inject()
    private val profilesStore: ProfilesStore by inject()
    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettings: NetworkSettingsStorage by inject()

    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.VPN_CONFIG)
    }

    private var notificationJob: Job? = null
    private var serviceScope: CoroutineScope? = null

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunFd: Int? = null
    private var httpProxyAddress: InetSocketAddress? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            ServiceNotificationManager.VPN_CONFIG.notificationId,
            notificationManager.create("正在连接...", "正在建立连接", false)
        )

        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    startVpn(profileId)
                } else {
                    stopSelf()
                }
            }

            ACTION_STOP -> stopVpn()
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startVpn(profileId: String) {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        serviceScope?.launch {
            try {

                val profile = profilesStore.getAllProfiles().find { it.id == profileId }
                if (profile == null) {
                    showErrorNotification("启动失败", "配置文件不存在")
                    return@launch
                }


                val loadResult = clashManager.loadProfile(profile)
                if (loadResult.isFailure) {
                    val error = loadResult.exceptionOrNull()
                    showErrorNotification("启动失败", error?.message ?: "配置加载失败")
                    return@launch
                }

                vpnInterface = withContext(Dispatchers.IO) { establishVpnInterface() }
                    ?: run {
                        showErrorNotification("启动失败", "无法建立 VPN 连接")
                        return@launch
                    }

                val pfd = vpnInterface!!
                val rawFd = pfd.detachFd()

                runCatching { pfd.close() }
                vpnInterface = null

                val config = Configuration.TunConfig()
                val tunConfig = config.copy(
                    stack = networkSettings.tunStack.value.name.lowercase(),
                    dnsHijacking = networkSettings.dnsHijack.value
                )

                clashManager.startTun(
                    fd = rawFd,
                    config = tunConfig,
                    enableIPv6 = networkSettings.enableIPv6.value,
                    markSocket = { protect(it) }
                )

                startNotificationUpdate()
            } catch (e: Exception) {
                showErrorNotification("启动失败", e.message ?: "未知错误")
            }
        }
    }

    private fun startNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = notificationManager.startTrafficUpdate(
            serviceScope!!, clashManager, appSettingsStorage
        )
    }

    private fun stopNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = null
    }

    private fun buildErrorNotification(title: String, content: String): Notification {
        return notificationManager.create(title, content, false)
    }

    private fun showNotificationAndStopService(notification: Notification, notificationId: Int) {
        startForeground(notificationId, notification)
        serviceScope?.launch {
            delay(3000)
            stopSelf()
        }
    }

    private fun showErrorNotification(title: String, content: String) {
        val notification = buildErrorNotification(title, content)
        showNotificationAndStopService(notification, ServiceNotificationManager.VPN_CONFIG.notificationId)
    }

    private fun listenHttp(): InetSocketAddress? {
        val r = { 1 + random.nextInt(199) }
        val listenAt = "127.${r()}.${r()}.${r()}:0"
        val address = Clash.startHttp(listenAt)
        return address?.let { parseInetSocketAddress(it) }
    }

    private fun parseInetSocketAddress(address: String): InetSocketAddress {
        val lastColon = address.lastIndexOf(':')
        val host = address.substring(0, lastColon)
        val port = address.substring(lastColon + 1).toInt()
        return InetSocketAddress(host, port)
    }

    private fun establishVpnInterface(): ParcelFileDescriptor? = runCatching {
        val config = Configuration.TunConfig()
        Builder().apply {
            setSession("YumeBox VPN")
            setMtu(config.mtu)
            setBlocking(false)
            addAddress(config.gateway, 30)

            if (networkSettings.enableIPv6.value) {
                addAddress(RouteConfig.TUN_GATEWAY6, RouteConfig.TUN_SUBNET_PREFIX6)
            }

            if (networkSettings.bypassPrivateNetwork.value) {
                RouteConfig.BYPASS_PRIVATE_ROUTES.forEach { cidr ->
                    val (addr, prefix) = RouteConfig.parseCidr(cidr)
                    addRoute(addr, prefix)
                }
                if (networkSettings.enableIPv6.value) {
                    RouteConfig.BYPASS_PRIVATE_ROUTES_V6.forEach { cidr ->
                        val (addr, prefix) = RouteConfig.parseCidr(cidr)
                        addRoute(addr, prefix)
                    }
                }
                addRoute(config.dns, 32)
                if (networkSettings.enableIPv6.value) addRoute(RouteConfig.TUN_DNS6, 128)
            } else {
                addRoute("0.0.0.0", 0)
                if (networkSettings.enableIPv6.value) addRoute("::", 0)
            }

            val accessControlPackages = networkSettings.accessControlPackages.value
            when (networkSettings.accessControlMode.value) {
                AccessControlMode.ALLOW_ALL -> {}
                AccessControlMode.ALLOW_SPECIFIC -> {
                    (accessControlPackages + packageName).forEach { runCatching { addAllowedApplication(it) } }
                }

                AccessControlMode.DENY_SPECIFIC -> {
                    (accessControlPackages - packageName).forEach { runCatching { addDisallowedApplication(it) } }
                }
            }

            addDnsServer(config.dns)
            if (networkSettings.enableIPv6.value) addDnsServer(RouteConfig.TUN_DNS6)
            if (networkSettings.allowBypass.value) allowBypass()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setMetered(false)

                if (networkSettings.systemProxy.value) {
                    listenHttp()?.let { addr ->
                        httpProxyAddress = addr
                        val exclusionList = if (networkSettings.bypassPrivateNetwork.value) {
                            RouteConfig.HTTP_PROXY_LOCAL_LIST + RouteConfig.HTTP_PROXY_BLACK_LIST
                        } else {
                            RouteConfig.HTTP_PROXY_BLACK_LIST
                        }
                        setHttpProxy(
                            ProxyInfo.buildDirectProxy(
                                addr.address.hostAddress,
                                addr.port,
                                exclusionList
                            )
                        )
                    }
                }
            }
        }.establish()
    }.getOrNull()

    private fun stopVpn() {
        stopNotificationUpdate()

        clashManager.stop()

        vpnInterface?.close()
        vpnInterface = null
        tunFd = null

        httpProxyAddress = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        stopVpn()
        super.onDestroy()
    }
}
