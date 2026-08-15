package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.component.Skeleton

@Composable
internal fun EventImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    var isImageLoaded by remember(url) { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (url.isBlank()) {
            EventImageSkeleton()
            return@Box
        }

        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onError = { isImageLoaded = false },
            onSuccess = { isImageLoaded = true },
        )
        if (!isImageLoaded) EventImageSkeleton()
    }
}

@Composable
private fun EventImageSkeleton() {
    Skeleton(
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
    )
}
