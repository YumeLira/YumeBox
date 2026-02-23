package com.github.yumelira.yumebox.service

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.util.sendBroadcastSelf
import com.github.yumelira.yumebox.service.runtime.util.sendOverrideChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.ConcurrentHashMap

class ClashManager(private val context: Context) : IClashManager,
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private data class ExternalSelectionCandidate(
        val node: String,
        val firstSeenAt: Long,
    )

    private companion object {
        const val EXTERNAL_SELECTION_CONFIRM_MS = 1200L
    }

    private val store = ServiceStore()
    private var logReceiver: ReceiveChannel<LogMessage>? = null
    private val externalCandidates = ConcurrentHashMap<String, ExternalSelectionCandidate>()

    override fun queryTunnelState(): TunnelState {
        return Clash.queryTunnelState()
    }

    override fun queryTrafficNow(): Long {
        return Clash.queryTrafficNow()
    }

    override fun queryTrafficTotal(): Long {
        return Clash.queryTrafficTotal()
    }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return Clash.queryGroupNames(excludeNotSelectable)
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        return Clash.queryGroup(name, proxySort).also { group ->
            syncSelectionSnapshotSafely(name, group)
        }
    }

    override fun queryConfiguration(): UiConfiguration {
        return Clash.queryConfiguration()
    }

    override fun queryProviders(): ProviderList {
        return ProviderList(Clash.queryProviders())
    }

    override fun queryOverride(slot: Clash.OverrideSlot): ConfigurationOverride {
        return Clash.queryOverride(slot)
    }

    override fun patchSelector(group: String, name: String): Boolean {
        return Clash.patchSelector(group, name).also {
            val current = store.activeProfile ?: return@also

            if (it) {
                SelectionDao.setSelected(Selection(current, group, name))
                externalCandidates.remove(selectionKey(current.toString(), group))
            } else {
                SelectionDao.remove(current, group)
                externalCandidates.remove(selectionKey(current.toString(), group))
            }
        }
    }

    override fun patchOverride(slot: Clash.OverrideSlot, configuration: ConfigurationOverride) {
        Clash.patchOverride(slot, configuration)

        context.sendOverrideChanged()
    }

    override fun clearOverride(slot: Clash.OverrideSlot) {
        Clash.clearOverride(slot)
    }

    override fun requestStop() {
        runCatching {
            context.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
        }

        runCatching {
            context.stopService(Intent(context, TunService::class.java))
            context.stopService(Intent(context, ClashService::class.java))
        }

        runCatching {
            Clash.stopHttp()
            Clash.stopTun()
            Clash.reset()
            Clash.clearOverride(Clash.OverrideSlot.Session)
        }
    }

    override suspend fun healthCheck(group: String) {
        return Clash.healthCheck(group).await()
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        return Clash.updateProvider(type, name).await()
    }

    private fun syncSelectionSnapshotSafely(group: String, proxyGroup: ProxyGroup) {
        val current = store.activeProfile ?: return
        val profileId = current.toString()
        val key = selectionKey(profileId, group)
        val node = proxyGroup.now.trim()
        val fallbackNode = proxyGroup.proxies.firstOrNull()?.name?.trim().orEmpty()
        val now = System.currentTimeMillis()
        if (node.isEmpty()) return

        val remembered = queryRememberedSelection(current, group)
        if (remembered == null) {
            SelectionDao.setSelected(Selection(current, group, node))
            externalCandidates.remove(key)
            return
        }
        if (remembered == node) {
            externalCandidates.remove(key)
            return
        }
        if (fallbackNode.isNotEmpty() && node == fallbackNode) {
            // Prevent startup/default fallback from overwriting remembered node.
            return
        }

        val candidate = externalCandidates[key]
        if (candidate == null || candidate.node != node) {
            externalCandidates[key] = ExternalSelectionCandidate(node = node, firstSeenAt = now)
            return
        }
        if (now - candidate.firstSeenAt < EXTERNAL_SELECTION_CONFIRM_MS) {
            return
        }
        SelectionDao.setSelected(Selection(current, group, node))
        externalCandidates.remove(key)
    }

    private fun selectionKey(profileId: String, group: String): String {
        return "$profileId::$group"
    }

    private fun queryRememberedSelection(profileId: java.util.UUID, group: String): String? {
        return SelectionDao.querySelections(profileId)
            .firstOrNull { it.proxy == group }
            ?.selected
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    override fun setLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()

                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver = Clash.subscribeLogcat().also { c ->
                    launch {
                        try {
                            while (isActive) {
                                observer.newItem(c.receive())
                            }
                        } catch (e: CancellationException) {
                            // intended behavior
                            // ignore
                        } catch (e: Exception) {
                            Log.w("UI crashed", e)
                        } finally {
                            withContext(NonCancellable) {
                                c.cancel()

                                Clash.forceGc()
                            }
                        }
                    }
                }
            }
        }
    }
}
