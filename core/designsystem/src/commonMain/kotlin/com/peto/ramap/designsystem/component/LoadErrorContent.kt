package com.peto.ramap.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoadErrorContent(
    image: DrawableResource,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = if (compact) 20.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                if (compact) 8.dp else 12.dp,
                Alignment.CenterVertically,
            ),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 148.dp else 220.dp),
        )
        AppText(
            text = title,
            style = AppTextStyle.H3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = description,
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            RetryButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
