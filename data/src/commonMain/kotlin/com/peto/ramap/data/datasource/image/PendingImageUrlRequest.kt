package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import kotlinx.coroutines.CompletableDeferred

internal class PendingImageUrlRequest(
    val image: SignedImageRequest,
    val access: ImageUrlAccessSnapshot,
    val result: CompletableDeferred<Result<SignedImageUrl?>>,
) {
    private var waiters = 0

    fun retain() {
        waiters++
    }

    fun release(): Boolean {
        waiters--
        return waiters == 0
    }
}
