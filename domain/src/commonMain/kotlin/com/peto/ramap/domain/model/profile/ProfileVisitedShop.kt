package com.peto.ramap.domain.model.profile

data class ProfileVisitedShop(
    val id: String,
    val name: String,
    val visitCount: Long,
    val instagramProfileImageUrl: String? = null,
    val isTied: Boolean = false,
)
