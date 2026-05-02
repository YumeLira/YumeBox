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

package com.github.yumelira.yumebox.core.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base ViewModel providing common state management patterns.
 *
 * This class encapsulates the standard MutableStateFlow → StateFlow pattern used across
 * the application, reducing boilerplate code in ViewModels.
 *
 * @param State The UI state data class managed by this ViewModel
 * @param initialState The initial state value
 *
 * Usage example:
 * ```kotlin
 * data class MyUiState(
 *     val isLoading: Boolean = false,
 *     val data: List<String> = emptyList(),
 *     val error: String? = null,
 *     val message: String? = null
 * )
 *
 * class MyViewModel : BaseViewModel<MyUiState>(MyUiState()) {
 *     fun loadData() {
 *         setLoading(true)
 *         viewModelScope.launch {
 *             try {
 *                 val result = repository.getData()
 *                 updateState { it.copy(data = result, isLoading = false) }
 *             } catch (error: Exception) {
 *                 showError(error.message ?: "Unknown error")
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel<State>(initialState: State) {

    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    /**
     * Updates the UI state using the provided transform function.
     *
     * @param transform Function that receives current state and returns new state
     */
    protected fun updateState(transform: (State) -> State) {
        _uiState.update(transform)
    }

    /**
     * Gets the current state value synchronously.
     */
    protected val currentState: State
        get() = _uiState.value
}

/**
 * Base ViewModel with built-in loading, error, and message state management.
 *
 * Use this when your state contains common UI patterns like loading indicators,
 * error messages, and transient user messages.
 *
 * Requirements: Your state class must implement [LoadableState] interface.
 *
 * Usage example:
 * ```kotlin
 * data class MyUiState(
 *     val data: List<String> = emptyList(),
 *     override val isLoading: Boolean = false,
 *     override val error: String? = null,
 *     override val message: String? = null
 * ) : LoadableState
 *
 * class MyViewModel : BaseLoadableViewModel<MyUiState>(MyUiState()) {
 *     fun loadData() {
 *         setLoading(true)
 *         viewModelScope.launch {
 *             repository.getData()
 *                 .onSuccess { data ->
 *                     updateState { it.copy(data = data) }
 *                     setLoading(false)
 *                     showMessage("Data loaded successfully")
 *                 }
 *                 .onFailure { error ->
 *                     showError(error.message ?: "Failed to load data")
 *                 }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseLoadableViewModel<State : LoadableState<State>>(initialState: State) :
    BaseViewModel<State>(initialState) {

    /**
     * Sets the loading state.
     */
    protected fun setLoading(loading: Boolean) {
        updateState { it.withLoading(loading) }
    }

    /**
     * Shows an error message and clears loading state.
     */
    protected fun showError(error: String) {
        updateState { it.withError(error).withLoading(false) }
    }

    /**
     * Shows a transient message to the user.
     */
    protected fun showMessage(message: String) {
        updateState { it.withMessage(message) }
    }

    /**
     * Clears the error message.
     */
    protected fun clearError() {
        updateState { it.withError(null) }
    }

    /**
     * Clears the transient message (typically after displaying it).
     */
    protected fun clearMessage() {
        updateState { it.withMessage(null) }
    }
}

/**
 * Interface for state classes that support loading, error, and message states.
 *
 * Implement this interface in your state data class to use [BaseLoadableViewModel].
 */
interface LoadableState<T : LoadableState<T>> {
    val isLoading: Boolean
    val error: String?
    val message: String?

    /**
     * Creates a copy of this state with updated loading status.
     */
    fun withLoading(loading: Boolean): T

    /**
     * Creates a copy of this state with updated error message.
     */
    fun withError(error: String?): T

    /**
     * Creates a copy of this state with updated transient message.
     */
    fun withMessage(message: String?): T
}
