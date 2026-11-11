package com.peto.ramap.designsystem.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_photo_description

@Composable
fun ReviewCardImages(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    photoViewerEnabled: Boolean = true,
    imageSize: Dp = 220.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    centerSingleImage: Boolean = true,
    onImageLoadFailed: (String) -> Unit = {},
    onImageLoadSucceeded: (String) -> Unit = {},
    imageRetryKeys: Map<String, Int> = emptyMap(),
) {
    val photos = remember(imageUrls) { imageUrls.mapIndexed { index, url -> ReviewGalleryPhoto(id = "$index:$url", url = url) } }
    ReviewPhotoGallery(
        photos = photos,
        modifier = modifier,
        photoViewerEnabled = photoViewerEnabled,
        imageSize = imageSize,
        contentPadding = contentPadding,
        centerSingleImage = centerSingleImage,
        onImageLoadFailed = onImageLoadFailed,
        onImageLoadSucceeded = onImageLoadSucceeded,
        imageRetryKeys = imageRetryKeys,
    )
}

@Composable
fun ReviewPhotoGallery(
    photos: List<ReviewGalleryPhoto>,
    modifier: Modifier = Modifier,
    photoViewerEnabled: Boolean = true,
    imageSize: Dp = 220.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    centerSingleImage: Boolean = true,
    onImageLoadFailed: (String) -> Unit = {},
    onImageLoadSucceeded: (String) -> Unit = {},
    imageRetryKeys: Map<String, Int> = emptyMap(),
) {
    if (photos.isEmpty()) return
    var viewedPhotoId by remember(photoViewerEnabled) { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
            if (photos.size == 1 && centerSingleImage) Arrangement.Center else Arrangement.spacedBy(4.dp),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            val url = photo.url
            key(imageRetryKeys[url] ?: 0) {
                ReviewPhoto(
                    url = url,
                    contentDescription =
                        stringResource(
                            Res.string.review_photo_description,
                            index + 1,
                        ),
                    modifier =
                        Modifier
                            .size(imageSize)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (photoViewerEnabled) {
                                    Modifier.clickable(role = Role.Button) { viewedPhotoId = photo.id }
                                } else {
                                    Modifier
                                },
                            ),
                    onLoadFailed = { onImageLoadFailed(url) },
                    onLoadSucceeded = { onImageLoadSucceeded(url) },
                )
            }
        }
    }
    val selectedIndex = photos.indexOfFirst { it.id == viewedPhotoId }
    if (selectedIndex >= 0) {
        ReviewPhotoViewer(
            photos = photos,
            initialPage = selectedIndex,
            onPhotoChanged = { viewedPhotoId = it },
            onDismiss = { viewedPhotoId = null },
        )
    } else if (viewedPhotoId != null) {
        LaunchedEffect(viewedPhotoId) { viewedPhotoId = null }
    }
}
