package com.peto.ramap.ui.main.map.archive.model

import coil3.PlatformContext
import coil3.request.ImageRequest
import com.peto.ramap.domain.model.review.MyReviewPhoto

internal class ArchiveMarkerImageRequestFactory {
    fun create(
        context: PlatformContext,
        photo: MyReviewPhoto?,
        sizePx: Int,
    ): ImageRequest =
        ImageRequest
            .Builder(context)
            .data(photo?.url)
            .size(sizePx)
            .memoryCacheKey(photo?.reference)
            .diskCacheKey(photo?.reference)
            .configureMarkerBitmapDecoding()
            .build()
}

internal expect fun ImageRequest.Builder.configureMarkerBitmapDecoding(): ImageRequest.Builder
