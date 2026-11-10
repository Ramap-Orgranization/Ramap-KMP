package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun ShopThumbnail(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {},
    topEndBadge: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .noRippleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            RemoteShopImage(
                url = imageUrl,
                modifier = Modifier.fillMaxSize(0.94f).align(Alignment.Center),
            )
            badge()
            topEndBadge()
        }
        AppText(
            text = name,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
