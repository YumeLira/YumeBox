package com.github.yumelira.yumebox.presentation.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Play
import com.github.yumelira.yumebox.presentation.icon.yume.Square
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProxyControlButton(
    isRunning: Boolean,
    isEnabled: Boolean,
    hasEnabledProfile: Boolean,
    hasProfiles: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val cornerRadius = AppConstants.UI.BUTTON_CORNER_RADIUS
    val buttonWidthFraction = 0.3f

    MiuixTheme.colorScheme.surface
    MiuixTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasProfiles) {
            HintText("请先添加配置文件")
        } else if (!hasEnabledProfile) {
            HintText("请先在「配置」页面启用一个配置")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    scaleAnim.animateTo(
                        targetValue = 0.92f,
                        animationSpec = tween(
                            durationMillis = 90,
                            easing = FastOutSlowInEasing
                        )
                    )
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                onClick()
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction)
                .scale(scaleAnim.value)
                .shadow(
                    elevation = 1.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius),
                    clip = false
                )
                .border(
                    width = 0.2.dp,
                    color = MiuixTheme.colorScheme.outline,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
                ),
            colors = ButtonDefaults.buttonColors(MiuixTheme.colorScheme.background),
            cornerRadius = cornerRadius,
            minHeight = 36.dp
        ) {
            Icon(
                imageVector = if (isRunning) Yume.Square else Yume.Play,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurface
            )
        }


    }
}


@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}
