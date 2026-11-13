package com.peto.ramap.domain.model.review

sealed interface ReviewAttachment {
    data class Existing(
        val path: String,
        val url: String?,
    ) : ReviewAttachment

    data class New(
        val image: ReviewImage,
    ) : ReviewAttachment
}
