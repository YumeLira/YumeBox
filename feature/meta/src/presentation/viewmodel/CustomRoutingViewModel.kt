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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomRoutingViewModel(
    private val overrideConfigRepository: OverrideConfigStore,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
) : ViewModel() {

    private val contentState = MutableStateFlow("")
    val content: StateFlow<String> = contentState.asStateFlow()

    init {
        viewModelScope.launch {
            contentState.value = overrideConfigRepository.loadCustomRoutingContent().orEmpty()
        }
    }

    suspend fun saveContent(updatedContent: String): Boolean {
        return runCatching {
            contentState.value = updatedContent
            overrideConfigRepository.saveCustomRoutingContent(updatedContent)
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(
                OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID,
            )
        }.getOrElse {
            false
        }
    }
}
