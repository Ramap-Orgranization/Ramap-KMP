package com.peto.ramap.designsystem.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun CommonTopBar(
    title: String,
    left: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    right: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
        ) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                left()
            }

            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                right()
            }

            AppText(
                text = title,
                style = AppTextStyle.H4Brand,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
