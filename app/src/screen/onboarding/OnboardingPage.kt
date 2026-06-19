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

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.github.yumelira.yumebox.screen.settings.component.ThemeModeSelectorItem
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StartupHeroShell(enabled: Boolean, onStart: () -> Unit) {
    OnboardingPageFrame {
        Spacer(modifier = Modifier.height(UiDp.dp212))

        RevealBlock(delayMillis = 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            StartupTypewriterWord(
                phrases = StartupTypewriterPhrases,
                modifier = Modifier.widthIn(max = UiDp.dp320),
            )
        }

        Spacer(modifier = Modifier.height(UiDp.dp18))

        DetailScrollableContent(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(UiDp.dp20))
        }

        RevealScaleBlock(
            delayMillis = 680,
            modifier = Modifier.align(Alignment.CenterHorizontally).offset(y = (-156).dp),
        ) {
            HeroStartButton(enabled = enabled, onStart = onStart)
        }
    }
}

@Composable
internal fun ProvisionDetailShell(
    previewIcon: ImageVector,
    title: String,
    subtitle: String,
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    OnboardingPageFrame {
        Spacer(modifier = Modifier.height(UiDp.dp88))

        RevealBlock(delayMillis = 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            DetailPreviewBadge(icon = previewIcon)
        }

        Spacer(modifier = Modifier.height(UiDp.dp32))

        DetailHeadline(title = title, subtitle = subtitle)

        Spacer(modifier = Modifier.height(UiDp.dp40))

        DetailScrollableContent(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp18),
        ) {
            RevealBlock(delayMillis = 160) {
                Column(verticalArrangement = Arrangement.spacedBy(UiDp.dp18), content = content)
            }
            Spacer(modifier = Modifier.height(UiDp.dp20))
        }

        DetailFooter(delayMillis = 220, offsetY = (-36).dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
            ) {
                SecondaryFooterAction(
                    text = MLang.Onboarding.Navigation.Back,
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PrimaryFooterAction(
                    text = primaryText,
                    enabled = primaryEnabled,
                    onClick = onPrimaryClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailBackdrop()

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = PagePadding, vertical = UiDp.dp12),
            content = content,
        )
    }
}

@Composable
private fun DetailScrollableContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = DetailWidth)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
internal fun PermissionContent(state: PermissionState) {
    val notificationSummary =
        when {
            state.notificationGranted -> MLang.Onboarding.Permission.Common.Granted
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ->
                MLang.Onboarding.Permission.Notification.SummaryNeed
            else -> MLang.Onboarding.Permission.Notification.SummaryNotRequired
        }

    DetailGroup {
        PermissionRow(
            icon = Yume.Message,
            title = MLang.Onboarding.Permission.Notification.Title,
            summary = notificationSummary,
            granted = state.notificationGranted,
            onClick = {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    state.onRequestNotification()
                    return@PermissionRow
                }
                if (!state.notificationGranted) {
                    state.onRequestNotification()
                }
            },
        )
        DetailDivider()
        PermissionRow(
            icon = Yume.List,
            title = MLang.Onboarding.Permission.AppList.Title,
            summary =
                if (state.appListGranted) {
                    MLang.Onboarding.Permission.Common.Granted
                } else {
                    MLang.Onboarding.Permission.AppList.SummaryNeed
                },
            granted = state.appListGranted,
            onClick = {
                if (!state.appListGranted) {
                    state.onRequestAppList()
                }
            },
        )
    }
}

@Composable
internal fun TermsContent(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onPrivacySheetRequest: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val linkStyle =
        remember(colorScheme.primary) {
            SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    val linkStyles = remember(linkStyle) { TextLinkStyles(style = linkStyle) }
    val annotatedText =
        remember(linkStyles, onPrivacySheetRequest) {
            buildAnnotatedString {
                append(MLang.Onboarding.Privacy.RichTextLead)
                append(" ")
                append(MLang.Onboarding.Privacy.RichTextPrefix)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = LinkTermsTag,
                        styles = linkStyles,
                        linkInteractionListener = { onPrivacySheetRequest() },
                    )
                ) {
                    withStyle(linkStyle) { append(MLang.Onboarding.Privacy.TermsLink) }
                }
                append(MLang.Onboarding.Privacy.RichTextConnector)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = LinkPolicyTag,
                        styles = linkStyles,
                        linkInteractionListener = { onPrivacySheetRequest() },
                    )
                ) {
                    withStyle(linkStyle) { append(MLang.Onboarding.Privacy.PolicyLink) }
                }
                append(MLang.Onboarding.Privacy.RichTextSuffix)
            }
        }

    DetailGroup {
        Column(
            modifier = Modifier.padding(horizontal = UiDp.dp18, vertical = UiDp.dp18),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp14),
        ) {
            Text(
                text = annotatedText,
                style =
                    MiuixTheme.textStyles.body2.copy(
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
            ) {
                Checkbox(
                    state = androidx.compose.ui.state.ToggleableState(accepted),
                    onClick = { onAcceptedChange(!accepted) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp4),
                ) {
                    Text(
                        text = MLang.Onboarding.Privacy.Accept.Title,
                        style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PersonalizeContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onShowThemeColorPickerChange: (Boolean) -> Unit,
) {
    DetailGroup {
        ThemeModeSelectorItem(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
        DetailDivider()
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = {},
            showBottomSheetInPlace = false,
            onOpenPickerRequest = { onShowThemeColorPickerChange(true) },
        )
    }
}

@Composable
internal fun FinishHeroShell(
    enabled: Boolean,
    onPrimaryClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCommunityClick: () -> Unit,
) {
    OnboardingPageFrame {
        Spacer(modifier = Modifier.height(UiDp.dp88))

        RevealBlock(delayMillis = 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            DetailPreviewBadge(icon = Yume.CircleCheckBig)
        }

        Spacer(modifier = Modifier.height(UiDp.dp32))

        DetailHeadline(
            title = MLang.Onboarding.Finish.Title,
            subtitle = MLang.Onboarding.Finish.Subtitle,
        )

        Spacer(modifier = Modifier.height(UiDp.dp40))

        DetailScrollableContent(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp18),
        ) {
            RevealBlock(delayMillis = 160) {
                DetailGroup {
                    ProjectLinkRow(
                        icon = Yume.Github,
                        title = MLang.Onboarding.Project.Github.Title,
                        summary = MLang.Onboarding.Project.Github.Summary,
                        onClick = onGithubClick,
                    )
                    DetailDivider()
                    ProjectLinkRow(
                        icon = Yume.Message,
                        title = MLang.Onboarding.Project.Community.Title,
                        summary = MLang.Onboarding.Project.Community.Summary,
                        onClick = onCommunityClick,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp20))
        }

        DetailFooter(delayMillis = 220, offsetY = (-36).dp) {
            PrimaryFooterAction(
                text = MLang.Onboarding.Navigation.Enter,
                enabled = enabled,
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.DetailHeadline(title: String, subtitle: String) {
    Column(
        modifier =
            Modifier.widthIn(max = DetailWidth).fillMaxWidth().align(Alignment.CenterHorizontally),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
    ) {
        RevealBlock(delayMillis = 50) {
            Text(
                text = title,
                style =
                    MiuixTheme.textStyles.title2.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        RevealBlock(delayMillis = 110) {
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.body2.copy(lineHeight = 22.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ColumnScope.DetailFooter(
    delayMillis: Int,
    offsetY: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    RevealBlock(
        delayMillis = delayMillis,
        modifier =
            Modifier.widthIn(max = DetailWidth)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .offset(y = offsetY),
    ) {
        content()
    }
}
