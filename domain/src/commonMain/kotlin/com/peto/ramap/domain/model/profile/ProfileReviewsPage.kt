package com.peto.ramap.domain.model.profile

import com.peto.ramap.domain.model.review.ProfileReview

data class ProfileReviewsPage(
    val access: ProfileAccess,
    val reviews: List<ProfileReview>,
)
