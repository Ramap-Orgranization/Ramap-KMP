package com.peto.ramap.designsystem.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_progress

@Composable
fun RemoteShopImage(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    var isImageLoaded by remember(url) { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .border(width = 1.dp, color = GrayColor.C100, shape = shape)
                .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            ShopImagePlaceholder()
            return@Box
        }

        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onError = { isImageLoaded = false },
            onSuccess = { isImageLoaded = true },
        )
        if (!isImageLoaded) ShopImagePlaceholder()
    }
}

@Composable
private fun ShopImagePlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_progress),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = SHOP_IMAGE_PLACEHOLDER_SCALE
                        scaleY = SHOP_IMAGE_PLACEHOLDER_SCALE
                    },
        )
    }
}

private const val SHOP_IMAGE_PLACEHOLDER_SCALE = 1.8f
