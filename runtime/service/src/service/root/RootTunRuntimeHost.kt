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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.service.root

import android.content.Context
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.runtime.session.RuntimeHost
import com.github.yumelira.yumebox.service.runtime.session.RuntimeSpec
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import com.github.yumelira.yumebox.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.service.runtime.util.sendClashStopped
import com.github.yumelira.yumebox.service.runtime.util.sendProfileLoaded
import java.util.UUID

internal class RootTunRuntimeHost(
    private val service: RootTunRootService,
    private val stateStore: RootTunStateStore,
) : RuntimeHost {
    private var startedBroadcastSent = false
    private var lastRuntimeSpec: RuntimeSpec? = null

    override val context: Context = service
    override val mode: ProxyMode = ProxyMode.RootTun

    override fun onStarting(spec: RuntimeSpec) {
        startedBroadcastSent = false
        lastRuntimeSpec = spec
        StatusProvider.clearLegacyStateFiles()
        StatusProvider.markRuntimeStarting(ProxyMode.RootTun)
    }

    override fun onStarted(spec: RuntimeSpec) {
        lastRuntimeSpec = spec
        StatusProvider.markRuntimeRunning(ProxyMode.RootTun)
        service.sendClashStarted()
        startedBroadcastSent = true
    }

    override fun onStopped(reason: String?) {
        StatusProvider.markRuntimeIdle(ProxyMode.RootTun)
        service.sendClashStopped(reason)
    }

    override fun onProfileLoaded(profileUuid: String) {
        service.sendProfileLoaded(UUID.fromString(profileUuid))
    }

    override fun onSnapshotChanged(snapshot: RuntimeSnapshot) {
        stateStore.updateStatus(snapshot.toRootTunStatus(lastRuntimeSpec))
        if (snapshot.phase == RuntimePhase.Running && !startedBroadcastSent) {
            service.sendClashStarted()
            startedBroadcastSent = true
        }
    }

    override fun onLogReady(ready: Boolean) {
        val current = stateStore.snapshot()
        stateStore.updateStatus(
            current.copy(controllerReady = true, runtimeReady = ready || current.runtimeReady)
        )
    }

    override fun onLogItem(log: LogMessage) = Unit

    override fun reportFailure(error: String) {
        StatusProvider.markRuntimeFailed(ProxyMode.RootTun)
        stateStore.updateStatus(
            stateStore
                .snapshot()
                .copy(
                    state = RootTunState.Failed,
                    running = false,
                    lastError = error,
                    runtimeReady = false,
                )
        )
        service.sendClashStopped(error)
    }

    private fun RuntimeSnapshot.toRootTunStatus(spec: RuntimeSpec?): RootTunStatus {
        val state =
            when (phase) {
                RuntimePhase.Idle -> RootTunState.Idle
                RuntimePhase.Starting -> RootTunState.Starting
                RuntimePhase.Running -> RootTunState.Running
                RuntimePhase.Stopping -> RootTunState.Stopping
                RuntimePhase.Failed -> RootTunState.Failed
            }
        return RootTunStatus(
            state = state,
            running = state.isActive,
            lastError = lastError,
            profileUuid = profileUuid,
            profileName = profileName,
            runtimeReady = phase == RuntimePhase.Running,
            controllerReady = true,
            startedAt = startedAt,
            staticPlanFingerprint = spec?.staticPlanFingerprint,
            transportFingerprint = spec?.transportFingerprint,
            overrideFingerprint = effectiveFingerprint ?: spec?.effectiveFingerprint,
            profileFingerprint = spec?.profileFingerprint,
        )
    }
}
