package com.peto.ramap.ui.main.map.archive.model

import coil3.PlatformContext
import coil3.request.CachePolicy
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
            .diskCachePolicy(CachePolicy.DISABLED)
            .configureMarkerBitmapDecoding()
            .build()
}

internal expect fun ImageRequest.Builder.configureMarkerBitmapDecoding(): ImageRequest.Builder
