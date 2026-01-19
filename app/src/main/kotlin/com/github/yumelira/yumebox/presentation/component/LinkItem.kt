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

package com.github.yumelira.yumebox.presentation.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.openUrl
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.extra.SuperArrow

@Composable
fun LinkItem(
    title: String,
    url: String,
    showArrow: Boolean = false,
    context: Context = LocalContext.current,
) {
    if (showArrow) {
        SuperArrow(
            title = title,
            summary = url,
            onClick = { openUrl(context, url) }
        )
    } else {
        BasicComponent(
            title = title,
            summary = url,
            onClick = { openUrl(context, url) }
        )
    }
}
