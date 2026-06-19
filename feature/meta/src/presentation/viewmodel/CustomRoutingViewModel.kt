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

package com.github.yumelira.yumebox.feature.meta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.core.util.YamlCodec
import com.github.yumelira.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumelira.yumebox.data.store.OverrideConfigStore
import com.github.yumelira.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumelira.yumebox.feature.meta.presentation.util.OverridePresetTemplateSelection
import com.github.yumelira.yumebox.feature.meta.presentation.util.analyzePresetTemplateContent
import com.github.yumelira.yumebox.feature.meta.presentation.util.buildPresetTemplateYaml
import com.github.yumelira.yumebox.feature.meta.presentation.util.defaultOverridePresetTemplateSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class CustomRoutingViewModel(
    private val overrideConfigRepository: OverrideConfigStore,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
    private val customRoutingBootstrapper: CustomRoutingBootstrapper,
) : ViewModel() {

    private val presetSelectionState = MutableStateFlow(defaultOverridePresetTemplateSelection())
    val presetSelection: StateFlow<OverridePresetTemplateSelection> =
        presetSelectionState.asStateFlow()

    private val customRoutingContentState = MutableStateFlow("")
    val customRoutingContent: StateFlow<String> = customRoutingContentState.asStateFlow()

    private val templateRoundTripSafeState = MutableStateFlow(true)
    val templateRoundTripSafe: StateFlow<Boolean> = templateRoundTripSafeState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { reloadStateFromStoredContent() }
                .onFailure {
                    Timber.e(it, "Failed to reload custom routing state from stored content")
                }
        }
    }

    suspend fun savePresetSelection(
        updatedPresetSelection: OverridePresetTemplateSelection
    ): Result<Unit> {
        return runCatching {
            val generatedYaml = buildPresetTemplateYaml(updatedPresetSelection)
            overrideConfigRepository.saveCustomRoutingContent(generatedYaml)
            applyContentState(generatedYaml)
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(
                OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
            )
        }
    }

    suspend fun saveCustomRoutingYaml(content: String): Result<Unit> {
        return runCatching {
            val contentToSave =
                if (content.isBlank()) {
                    buildPresetTemplateYaml(defaultOverridePresetTemplateSelection())
                } else {
                    YamlCodec.validate(content)
                    content
                }
            overrideConfigRepository.saveCustomRoutingContent(contentToSave)
            applyContentState(contentToSave)
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(
                OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
            )
        }
    }

    private suspend fun reloadStateFromStoredContent() {
        applyContentState(customRoutingBootstrapper.ensureDefaultContent())
    }

    private fun applyContentState(content: String?) {
        val analysis = analyzePresetTemplateContent(content)
        customRoutingContentState.value = content.orEmpty()
        presetSelectionState.value = analysis.selection
        templateRoundTripSafeState.value = analysis.matchesTemplateExactly
    }
}
