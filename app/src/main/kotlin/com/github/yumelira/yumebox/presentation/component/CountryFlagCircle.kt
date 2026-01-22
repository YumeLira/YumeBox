package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.github.yumelira.yumebox.common.util.LocaleUtil

@Composable
fun CountryFlagCircle(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val flagUrl = remember(countryCode) { LocaleUtil.normalizeFlagUrl(countryCode) }
    Image(
        painter = rememberAsyncImagePainter(model = flagUrl),
        contentDescription = "$countryCode flag",
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}
