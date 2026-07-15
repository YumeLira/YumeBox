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

package com.github.yumelira.yumebox.runtime.api

object Intents {
    private fun intentAction(packageName: String, actionName: String): String =
        "$packageName.intent.action.$actionName"

    fun actionStartClash(packageName: String): String = "$packageName.action.START_CLASH"

    fun actionStopClash(packageName: String): String = "$packageName.action.STOP_CLASH"

    fun actionServiceRecreated(packageName: String): String =
        intentAction(packageName, "CLASH_RECREATED")

    fun actionClashStarted(packageName: String): String = intentAction(packageName, "CLASH_STARTED")

    fun actionClashStopped(packageName: String): String = intentAction(packageName, "CLASH_STOPPED")

    fun actionClashRequestStop(packageName: String): String =
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

    val ACTION_CLASH_STARTED: String
        get() = actionClashStarted(packageName)

    val ACTION_CLASH_STOPPED: String
        get() = actionClashStopped(packageName)

    val ACTION_CLASH_REQUEST_STOP: String
        get() = actionClashRequestStop(packageName)

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
}
