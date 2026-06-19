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

package com.github.yumelira.yumebox.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Utility for consolidating multiple scattered state variables into a single UiState data class.
 *
 * This helps reduce recomposition overhead when screens have many independent state variables.
 *
 * Instead of:
 * ```kotlin
 * var dialogShown by remember { mutableStateOf(false) }
 * var dialogTitle by remember { mutableStateOf("") }
 * var dialogMessage by remember { mutableStateOf("") }
 * var selectedItem by remember { mutableStateOf<Item?>(null) }
 * var isEditing by remember { mutableStateOf(false) }
 * // ... 50+ more state variables
 * ```
 *
 * Use:
 * ```kotlin
 * data class ScreenUiState(
 *     val dialogShown: Boolean = false,
 *     val dialogTitle: String = "",
 *     val dialogMessage: String = "",
 *     val selectedItem: Item? = null,
 *     val isEditing: Boolean = false,
 * )
 *
 * var uiState by rememberScreenState { ScreenUiState() }
 *
 * // Update single field
 * uiState = uiState.copy(dialogShown = true)
 *
 * // Update multiple fields
 * uiState = uiState.copy(
 *     dialogShown = true,
 *     dialogTitle = "Confirm",
 *     dialogMessage = "Are you sure?"
 * )
 * ```
 *
 * Benefits:
 * - Reduces recomposition scope (only composables reading changed fields recompose)
 * - Easier to track state changes
 * - Better IDE support (autocomplete, refactoring)
 * - Cleaner code
 */
@Composable
fun <T> rememberScreenState(initializer: () -> T): androidx.compose.runtime.MutableState<T> {
    return remember { mutableStateOf(initializer()) }
}

/**
 * Extension function to update state immutably.
 *
 * Usage:
 * ```kotlin
 * var state by rememberScreenState { MyUiState() }
 *
 * // Instead of: state = state.copy(loading = true)
 * state = state.update { copy(loading = true) }
 * ```
 */
inline fun <T> T.update(block: T.() -> T): T = block()
