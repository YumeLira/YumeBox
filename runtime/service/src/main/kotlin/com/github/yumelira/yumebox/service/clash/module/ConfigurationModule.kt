package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.util.importedDir
import com.github.yumelira.yumebox.service.runtime.util.sendProfileLoaded
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*

class ConfigurationModule(service: Service) : Module<ConfigurationModule.LoadException>(service) {
    data class LoadException(val message: String)

    private val store = ServiceStore()
    private val reload = Channel<Unit>(Channel.CONFLATED)

    override suspend fun run() {
        val broadcasts = receiveBroadcast {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
        }

        var loaded: UUID? = null

        reload.trySend(Unit)

        while (true) {
            val changed: UUID? = select {
                broadcasts.onReceive {
                    if (it.action == Intents.ACTION_PROFILE_CHANGED)
                        UUID.fromString(it.getStringExtra(Intents.EXTRA_UUID))
                    else
                        null
                }
                reload.onReceive {
                    null
                }
            }

            try {
                val current = store.activeProfile
                    ?: throw NullPointerException("No profile selected")

                if (current == loaded && changed != null && changed != loaded)
                    continue

                loaded = current

                val active = ImportedDao.queryByUUID(current)
                    ?: throw NullPointerException("No profile selected")

                Clash.load(service.importedDir.resolve(active.uuid.toString())).await()

                SelectionDao.querySelections(active.uuid).forEach { selection ->
                    var restored = false
                    repeat(3) { attempt ->
                        if (restored) return@repeat
                        if (Clash.patchSelector(selection.proxy, selection.selected)) {
                            restored = true
                            return@repeat
                        }
                        if (attempt < 2) {
                            delay(150)
                        }
                    }
                    if (!restored) {
                        Log.w("Restore selector failed: group=${selection.proxy}, selected=${selection.selected}")
                    }
                }

                StatusProvider.currentProfile = active.name

                service.sendProfileLoaded(current)

                Log.d("Profile ${active.name} loaded")
            } catch (e: Exception) {
                return enqueueEvent(LoadException(e.message ?: "Unknown"))
            }
        }
    }
}
