package com.peto.ramap.ui.main.map.archive.model

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.marker_visited_check
import ramap.shared.generated.resources.marker_visited_photo_frame

@Composable
internal fun rememberArchiveMarkerImages(
    shops: RamenShops,
    photos: Map<String, MyReviewPhoto>,
    failedPhotoReferences: Set<String>,
    onPhotoDownloadFailed: (String, String) -> Unit,
): Map<String, MapMarkerImage> {
    val requestFactory = remember { ArchiveMarkerImageRequestFactory() }
    val check = painterResource(Res.drawable.marker_visited_check)
    val frame = painterResource(Res.drawable.marker_visited_photo_frame)
    val images = mutableMapOf<String, MapMarkerImage>()
    for (shopId in shops.keys) {
        val marker =
            key(shopId) {
                rememberArchiveMarkerImage(
                    shopId = shopId,
                    photo = photos[shopId],
                    photoSigningFailed = photos[shopId]?.reference?.let { it in failedPhotoReferences } == true,
                    check = check,
                    frame = frame,
                    requestFactory = requestFactory,
                    onPhotoDownloadFailed = onPhotoDownloadFailed,
                )
            }
        if (marker != null) images[shopId] = marker
    }
    return images
}

@Composable
private fun rememberArchiveMarkerImage(
    shopId: String,
    photo: MyReviewPhoto?,
    photoSigningFailed: Boolean,
    check: Painter,
    frame: Painter,
    requestFactory: ArchiveMarkerImageRequestFactory,
    onPhotoDownloadFailed: (String, String) -> Unit,
): MapMarkerImage? {
    if (photo != null && photo.url == null && !photoSigningFailed) return null
    val availablePhoto = photo?.takeIf { it.url != null }
    val density = LocalDensity.current
    val context = LocalPlatformContext.current
    val request =
        remember(context, availablePhoto?.url, availablePhoto?.reference, density.density) {
            requestFactory.create(
                context = context,
                photo = availablePhoto,
                sizePx = (58f * density.density).toInt(),
            )
        }
    val imagePainter = rememberAsyncImagePainter(model = request)
    val state by imagePainter.state.collectAsState()
    LaunchedEffect(state, availablePhoto?.url) {
        if (state is AsyncImagePainter.State.Error && availablePhoto != null) onPhotoDownloadFailed(shopId, availablePhoto.reference)
    }
    if (availablePhoto != null && state !is AsyncImagePainter.State.Success && state !is AsyncImagePainter.State.Error) return null
    val loadedPhoto = if (availablePhoto != null) (state as? AsyncImagePainter.State.Success)?.painter else null
    val shape = if (loadedPhoto != null) frame else check
    return remember(loadedPhoto, shape, density, photo?.reference, photo?.url) {
        val painter = ArchiveMarkerPainter(shape = shape, photo = loadedPhoto)
        val size = painter.intrinsicSize
        val bitmap = ImageBitmap((size.width * density.density).toInt(), (size.height * density.density).toInt())
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            with(painter) { draw(size = this@draw.size) }
        }
        MapMarkerImage(
            bitmap = bitmap,
            anchorY = if (loadedPhoto == null) 57f / 62f else 74f / 82f,
            key = "${photo?.reference}:${photo?.url}:${loadedPhoto != null}",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveMarkerImagesPreview(
    @PreviewParameter(RamenShopsPreviewParameterProvider::class) shops: RamenShops,
) {
    RamapTheme {
        val photos =
            remember {
                mapOf(
                    "1" to MyReviewPhoto(url = "https://example.com/photo1.jpg", reference = "ref1"),
                )
            }
        val images =
            rememberArchiveMarkerImages(
                shops = shops,
                photos = photos,
                failedPhotoReferences = emptySet(),
                onPhotoDownloadFailed = { _, _ -> },
            )
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            images.values.forEach { markerImage ->
                Image(
                    bitmap = markerImage.bitmap,
                    contentDescription = null,
                )
            }
        }
    }
}
