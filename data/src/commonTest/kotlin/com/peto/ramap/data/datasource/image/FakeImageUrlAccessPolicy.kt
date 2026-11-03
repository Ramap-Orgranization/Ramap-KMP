package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest

internal class FakeImageUrlAccessPolicy : ImageUrlAccessPolicy {
    var access = ImageUrlAccessSnapshot("viewer", 0, 0)

    override fun snapshot(): ImageUrlAccessSnapshot = access

    override fun invalidatedImages(
        snapshot: ImageUrlAccessSnapshot,
        images: Collection<SignedImageRequest>,
    ): Set<SignedImageRequest> = if (snapshot == access) emptySet() else images.toSet()
}
