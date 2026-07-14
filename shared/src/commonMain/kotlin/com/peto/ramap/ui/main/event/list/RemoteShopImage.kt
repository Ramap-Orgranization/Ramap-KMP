package com.peto.ramap.ui.main.event.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.marker_ramen

@Composable
internal fun RemoteShopImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(GrayColor.C100), contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) {
            ShopImagePlaceholder()
            return@Box
        }

        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        ) {
            val state by painter.state.collectAsState()
            if (state is AsyncImagePainter.State.Success) {
                SubcomposeAsyncImageContent()
            } else {
                ShopImagePlaceholder()
            }
        }
    }
}

@Composable
private fun ShopImagePlaceholder() {
    Image(
        painter = painterResource(Res.drawable.marker_ramen),
        contentDescription = null,
        modifier = Modifier.size(28.dp),
    )
}
