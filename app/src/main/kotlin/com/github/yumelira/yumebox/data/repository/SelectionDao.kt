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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.yumelira.yumebox.data.model.Selection

class SelectionDao(context: Context) {
    companion object {
        private const val PREFS_NAME = "proxy_selections"
        private const val KEY_PREFIX = "selection_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setSelected(selection: Selection) {
        try {
            val key = makeKey(selection.profileId, selection.proxyGroup)
            prefs.edit {
                putString(key, selection.selectedNode)
            }
        } catch (_: Exception) {
        }
    }

    fun getAllSelections(profileId: String): Map<String, String> {
        return try {
            val prefix = makeKeyPrefix(profileId)
            val selections = mutableMapOf<String, String>()

            prefs.all.forEach { (key, value) ->
                if (key.startsWith(prefix) && value is String) {
                    val proxyGroup = key.substring(prefix.length)
                    selections[proxyGroup] = value
                }
            }

            selections
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun makeKey(profileId: String, proxyGroup: String): String {
        return "${KEY_PREFIX}${profileId}_$proxyGroup"
    }

    private fun makeKeyPrefix(profileId: String): String {
        return "${KEY_PREFIX}${profileId}_"
    }
}
