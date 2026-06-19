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

package com.github.yumelira.yumebox.feature.meta.presentation.util

import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.data.store.OverrideConfigStore

class CustomRoutingBootstrapper(private val overrideConfigRepository: OverrideConfigStore) {

    suspend fun ensureDefaultContent(): String {
        val existingContent = overrideConfigRepository.loadCustomRoutingContent()
        if (!existingContent.isNullOrBlank()) {
            // Content file exists, but its metadata entry may be missing (e.g. content was
            // written without going through save()). The compile pipeline keeps the custom
            // routing override in a profile chain only when the metadata entry exists, so
            // re-save here to heal the metadata when it is absent.
            val hasMetadata =
                overrideConfigRepository.getById(
                    OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
                ) != null
            if (!hasMetadata) {
                overrideConfigRepository.saveCustomRoutingContent(existingContent)
            }
            return existingContent
        }

        val generatedYaml = buildPresetTemplateYaml(defaultOverridePresetTemplateSelection())
        overrideConfigRepository.saveCustomRoutingContent(generatedYaml)
        return generatedYaml
    }
}
