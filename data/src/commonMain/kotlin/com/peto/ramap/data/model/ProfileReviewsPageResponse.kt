package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileReviewsPageResponse(
    val access: ProfileAccessResponse,
    val reviews: List<ReviewResponse> = emptyList(),
)
