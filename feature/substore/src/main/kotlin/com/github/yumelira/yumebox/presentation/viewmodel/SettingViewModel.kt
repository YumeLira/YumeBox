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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.repository.FeatureSettingsRepository
import com.github.yumelira.yumebox.substore.SubStoreService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettingViewModel(
    private val repository: FeatureSettingsRepository,
) : ViewModel() {

    val allowLanAccess = repository.allowLanAccess
    val backendPort = repository.backendPort
    val frontendPort = repository.frontendPort

    private val _events = MutableSharedFlow<SettingEvent>()
    val events: SharedFlow<SettingEvent> = _events.asSharedFlow()

    val isSubStoreRunning: Boolean
        get() = SubStoreService.isRunning

    fun onSubStoreCardClicked() {
        if (!isSubStoreRunning) return
        val host = currentHost()
        val frontendUrl = buildUrl(host, frontendPort.value)
        val backendUrl = buildUrl(host, backendPort.value)
        emitEvent(SettingEvent.OpenWebView("$frontendUrl/subs?api=$backendUrl"))
    }

    private fun currentHost(): String = if (allowLanAccess.value) "0.0.0.0" else "127.0.0.1"

    private fun buildUrl(host: String, port: Int): String = "http://$host:$port"

    private fun emitEvent(event: SettingEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

sealed interface SettingEvent {
    data class OpenWebView(val url: String) : SettingEvent
}
