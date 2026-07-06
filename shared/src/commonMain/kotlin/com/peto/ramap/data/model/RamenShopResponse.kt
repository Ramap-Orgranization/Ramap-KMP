package com.peto.ramap.data.model

import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MenuCategories
import com.peto.ramap.domain.model.RamenShop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RamenShopResponse(
    val id: String,
    @SerialName("kakao_place_id")
    val kakaoPlaceId: String? = null,
    @SerialName("naver_place_id")
    val naverPlaceId: String? = null,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    @SerialName("kakao_place_url")
    val kakaoPlaceUrl: String? = null,
    @SerialName("naver_place_url")
    val naverPlaceUrl: String? = null,
    val phone: String? = null,
    @SerialName("business_hours")
    val businessHours: String? = null,
    @SerialName("instagram_url")
    val instagramUrl: String? = null,
    @SerialName("kakao_rating")
    val kakaoRating: Double? = null,
    @SerialName("menu_category_ids")
    val menuCategoryIds: List<String>? = null,
    @SerialName("is_visible")
    val isVisible: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
) {
    fun toDomain(): RamenShop =
        RamenShop(
            id = id,
            kakaoPlaceId = kakaoPlaceId,
            naverPlaceId = naverPlaceId,
            name = name,
            address = address,
            location = Location(lat = lat, lng = lng),
            kakaoPlaceUrl = kakaoPlaceUrl,
            naverPlaceUrl = naverPlaceUrl,
            phone = phone,
            businessHours = businessHours,
            instagramUrl = instagramUrl,
            kakaoRating = kakaoRating,
            menuCategories = MenuCategories(menuCategoryIds.orEmpty().mapNotNull(Category::fromId)),
            isVisible = isVisible ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
