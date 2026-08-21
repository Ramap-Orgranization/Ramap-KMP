package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
internal fun OperatingNoticeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    AppText(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = AppTextStyle.H3,
        color = GrayColor.C500,
    )
}
