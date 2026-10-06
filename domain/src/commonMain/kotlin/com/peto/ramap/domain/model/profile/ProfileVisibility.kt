package com.peto.ramap.domain.model.profile

data class ProfileVisibility(
    val isPublic: Boolean,
    val followersCanReadReviews: Boolean = true,
    val followersCanReadSavedShops: Boolean = true,
)
