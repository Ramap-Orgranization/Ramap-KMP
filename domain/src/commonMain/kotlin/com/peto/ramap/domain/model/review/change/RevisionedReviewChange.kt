package com.peto.ramap.domain.model.review.change

data class RevisionedReviewChange(
    val revision: Long,
    val change: ReviewChange,
)
