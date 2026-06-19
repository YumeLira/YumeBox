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

package com.github.yumelira.yumebox.feature.editor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigEditorViewModel : ViewModel() {

    private val _session = MutableStateFlow(EditorSession())
    val session: StateFlow<EditorSession> = _session.asStateFlow()

    fun loadConfig(configId: String, configType: ConfigType, initialContent: String) {
        _session.value =
            EditorSession(
                configId = configId,
                configType = configType,
                savedContent = initialContent,
                draftContent = initialContent,
                isDirty = false,
                error = null,
            )
    }

    fun updateDraft(content: String) {
        _session.value =
            _session.value.let { session ->
                session.copy(draftContent = content, isDirty = content != session.savedContent)
            }
    }

    fun markPersisted(content: String) {
        _session.value =
            _session.value.copy(
                savedContent = content,
                draftContent = content,
                isDirty = false,
                error = null,
            )
    }

    fun setError(message: String?) {
        _session.value = _session.value.copy(error = message)
    }

    fun discardDraft() {
        _session.value =
            _session.value.let { session ->
                session.copy(draftContent = session.savedContent, isDirty = false, error = null)
            }
    }

    fun formatContent(content: String): String = content.trimIndent()
}

data class EditorSession(
    val configId: String? = null,
    val configType: ConfigType? = null,
    val savedContent: String = "",
    val draftContent: String = "",
    val isDirty: Boolean = false,
    val error: String? = null,
)

enum class ConfigType {
    Override,
    Profile,
}
