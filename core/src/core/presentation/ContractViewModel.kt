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

package com.github.yumelira.yumebox.core.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI contract base: strongly-typed one-shot effects/event channel for ViewModels. */
abstract class ContractViewModel<Effect : Any> : ViewModel() {
    private val _effect =
        MutableSharedFlow<Effect>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    protected suspend fun emitEffect(effect: Effect) {
        _effect.emit(effect)
    }

    protected fun tryEmitEffect(effect: Effect) {
        _effect.tryEmit(effect)
    }
}

/** Contract base with strongly-typed state and one-shot effect channel. */
abstract class ContractStateViewModel<State : LoadableState<State>, Effect : Any>(
    initialState: State
) : ContractViewModel<Effect>() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    protected fun updateState(transform: (State) -> State) {
        _uiState.update(transform)
    }

    protected fun setLoading(loading: Boolean) {
        updateState { it.withLoading(loading) }
    }

    protected fun postError(error: String, effect: Effect? = null) {
        updateState { it.withError(error).withLoading(false) }
        effect?.let(::tryEmitEffect)
    }

    protected fun postMessage(message: String, effect: Effect? = null) {
        updateState { it.withMessage(message) }
        effect?.let(::tryEmitEffect)
    }

    protected fun clearErrorState() {
        updateState { it.withError(null) }
    }

    protected fun clearMessageState() {
        updateState { it.withMessage(null) }
    }
}

/** AndroidViewModel variant for state/effect contract ViewModels requiring Application context. */
abstract class AndroidContractStateViewModel<State : LoadableState<State>, Effect : Any>(
    application: Application,
    initialState: State,
) : AndroidViewModel(application) {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _effect =
        MutableSharedFlow<Effect>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    protected suspend fun emitEffect(effect: Effect) {
        _effect.emit(effect)
    }

    protected fun tryEmitEffect(effect: Effect) {
        _effect.tryEmit(effect)
    }

    protected fun updateState(transform: (State) -> State) {
        _uiState.update(transform)
    }

    protected fun setLoading(loading: Boolean) {
        updateState { it.withLoading(loading) }
    }

    protected fun postError(error: String, effect: Effect? = null) {
        updateState { it.withError(error).withLoading(false) }
        effect?.let(::tryEmitEffect)
    }

    protected fun postMessage(message: String, effect: Effect? = null) {
        updateState { it.withMessage(message) }
        effect?.let(::tryEmitEffect)
    }

    protected fun clearErrorState() {
        updateState { it.withError(null) }
    }

    protected fun clearMessageState() {
        updateState { it.withMessage(null) }
    }
}
