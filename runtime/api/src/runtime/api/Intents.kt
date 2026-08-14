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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.runtime.api


object Intents {
    private fun intentAction(packageName: String, actionName: String): String =
        "$packageName.intent.action.$actionName"

    fun actionStartRuntime(packageName: String): String = "$packageName.action.START_CLASH"

    fun actionStopRuntime(packageName: String): String = "$packageName.action.STOP_CLASH"

    fun actionServiceRecreated(packageName: String): String =
        intentAction(packageName, "CLASH_RECREATED")

    fun actionRuntimeStarted(packageName: String): String =
        intentAction(packageName, "CLASH_STARTED")

    fun actionRuntimeStopped(packageName: String): String =
        intentAction(packageName, "CLASH_STOPPED")

    fun actionRuntimeRequestStop(packageName: String): String =
        intentAction(packageName, "CLASH_REQUEST_STOP")

    fun actionProfileChanged(packageName: String): String =
        intentAction(packageName, "PROFILE_CHANGED")

    fun actionProfileLoaded(packageName: String): String =
        intentAction(packageName, "PROFILE_LOADED")

    fun actionOverrideChanged(packageName: String): String =
        intentAction(packageName, "OVERRIDE_CHANGED")

    fun actionRootRuntimeFailed(packageName: String): String =
        intentAction(packageName, "ROOT_RUNTIME_FAILED")

    val ACTION_SERVICE_RECREATED: String
        get() = actionServiceRecreated(packageName)

    val ACTION_RUNTIME_STARTED: String
        get() = actionRuntimeStarted(packageName)

    val ACTION_RUNTIME_STOPPED: String
        get() = actionRuntimeStopped(packageName)

    val ACTION_RUNTIME_REQUEST_STOP: String
        get() = actionRuntimeRequestStop(packageName)

    val ACTION_PROFILE_CHANGED: String
        get() = actionProfileChanged(packageName)

    val ACTION_PROFILE_LOADED: String
        get() = actionProfileLoaded(packageName)

    val ACTION_OVERRIDE_CHANGED: String
        get() = actionOverrideChanged(packageName)

    const val EXTRA_STOP_REASON = "stop_reason"
    const val EXTRA_RESTART = "restart"
    const val EXTRA_RUNTIME_MODE = "runtime_mode"
    const val EXTRA_UUID = "uuid"
    const val EXTRA_AFFECTS_RUNTIME = "affects_runtime"
}
