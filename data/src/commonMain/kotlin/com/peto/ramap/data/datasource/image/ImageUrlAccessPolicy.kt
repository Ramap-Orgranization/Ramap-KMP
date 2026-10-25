package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest

internal interface ImageUrlAccessPolicy {
    fun snapshot(): ImageUrlAccessSnapshot

    fun invalidatedImages(
        snapshot: ImageUrlAccessSnapshot,
        images: Collection<SignedImageRequest>,
    ): Set<SignedImageRequest>
}
