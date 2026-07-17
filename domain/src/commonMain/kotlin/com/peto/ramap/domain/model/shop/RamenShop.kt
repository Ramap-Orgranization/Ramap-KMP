package com.peto.ramap.domain.model.shop

data class RamenShop(
    val id: String,
    val kakaoPlaceId: String?,
    val naverPlaceId: String? = null,
    val name: String,
    val address: String,
    val location: Location,
    val kakaoPlaceUrl: String?,
    val naverPlaceUrl: String? = null,
    val phone: String?,
    val businessHours: String?,
    val instagramUrl: String?,
    val kakaoRating: Double?,
    val menuCategories: MenuCategories,
    val isVisible: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val instagramProfileImageUrl: String? = null,
) {
    val hasCategory: Boolean
        get() = menuCategories.hasCategory
}
