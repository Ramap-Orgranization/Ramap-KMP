package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.peto.ramap.designsystem.component.shimmer
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_photo_library

@Composable
internal fun ReviewPhoto(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val request =
        remember(url, context) {
            ImageRequest
                .Builder(context)
                .data(url)
                .crossfade(REVIEW_PHOTO_CROSSFADE_MILLIS)
                .build()
        }
    var imageState by remember(url) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(GrayColor.C050),
        contentAlignment = Alignment.Center,
    ) {
        if (imageState is AsyncImagePainter.State.Empty ||
            imageState is AsyncImagePainter.State.Loading
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .shimmer(
                            shape = shape,
                            baseColor = GrayColor.C100,
                            highlightColor = GrayColor.C050,
                        ),
            )
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
        )
        if (imageState is AsyncImagePainter.State.Error) {
            Image(
                painter = painterResource(Res.drawable.ic_photo_library),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(GrayColor.C300),
            )
        }
    }
}

private const val REVIEW_PHOTO_CROSSFADE_MILLIS = 200
