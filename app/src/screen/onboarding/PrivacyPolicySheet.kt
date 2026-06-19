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

package com.github.yumelira.yumebox.screen.onboarding

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@SuppressLint("LocalContextResourcesRead")
@Composable
internal fun PrivacyPolicySheet(show: MutableState<Boolean>) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val policyText = remember {
        runCatching {
                context.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use {
                    it.readText()
                }
            }
            .getOrElse { MLang.Onboarding.Sheet.LoadFailed }
    }

    AppActionBottomSheet(
        show = show.value,
        title = MLang.Onboarding.Sheet.PrivacyPolicyTitle,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().heightIn(max = UiDp.dp450).verticalScroll(scrollState)
        ) {
            Text(
                text = policyText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}
