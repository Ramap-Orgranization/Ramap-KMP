package com.peto.ramap.domain.model.community

data class ProfileReviewsPage(
    val access: ProfileAccess,
    val reviews: List<ProfileReview>,
)
