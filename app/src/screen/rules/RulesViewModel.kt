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

package com.github.yumeyucca.yumebox.screen.rules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.RuntimeRule
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class RulesUiState(
    val rules: List<RuntimeRule> = emptyList(),
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val error: String? = null,
    val toggleError: String? = null,
    val togglingIndexes: Set<Int> = emptySet(),
    val searchQuery: String = "",
)

class RulesViewModel(private val appContext: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()
    private val toggleMutex = Mutex()

    val filteredRules: StateFlow<List<RuntimeRule>> =
        _uiState
            .map { state ->
                val query = state.searchQuery.trim()
                state.rules.takeIf { query.isEmpty() } ?: state.rules.filter { it.matches(query) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().queryRules()
            }
                .onSuccess { rules ->
                    _uiState.update {
                        it.copy(rules = rules, isLoading = false, isRunning = true, error = null)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "queryRules failed")
                    _uiState.update {
                        it.copy(
                            rules = emptyList(),
                            isLoading = false,
                            isRunning = false,
                            error = error.message,
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Toggle a single rule. Switch ON means the rule is **enabled** (disabled=false). PATCH
     * /rules/disable uses { index: disabled }.
     */
    fun setRuleEnabled(index: Int, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleMutex.withLock {
                val disabled = !enabled
                val originalRule =
                    _uiState.value.rules.firstOrNull { it.index == index } ?: return@withLock
                _uiState.update { state ->
                    state.copy(
                        rules =
                            state.rules.map { rule ->
                                rule.takeUnless { it.index == index }
                                    ?: rule.copy(disabled = disabled)
                            },
                        togglingIndexes = state.togglingIndexes + index,
                        toggleError = null,
                    )
                }
                runCatching {
                    RuntimeAccess.connect(appContext)
                    RuntimeAccess.core().setRuleDisabled(originalRule, disabled)
                }
                    .onSuccess { confirmedRules ->
                        _uiState.update {
                            it.copy(
                                rules = confirmedRules,
                                togglingIndexes = it.togglingIndexes - index,
                                toggleError = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        Timber.w(error, "setRuleDisabled failed")
                        _uiState.update { state ->
                            state.copy(
                                rules =
                                    state.rules.map { rule ->
                                        rule.takeIf { it.index != index } ?: originalRule
                                    },
                                togglingIndexes = state.togglingIndexes - index,
                                toggleError = error.message ?: error.javaClass.simpleName,
                            )
                        }
                    }
            }
        }
    }

    fun consumeToggleError() {
        _uiState.update { it.copy(toggleError = null) }
    }

    private fun RuntimeRule.matches(query: String): Boolean =
        payload.contains(query, ignoreCase = true) ||
                type.contains(query, ignoreCase = true) ||
                proxy.contains(query, ignoreCase = true) ||
                index.toString().contains(query)
}
