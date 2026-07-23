package com.peto.ramap.data.model

import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.ShopRanking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopRankingResponse(
    val id: String,
    @SerialName("kakao_place_id") val kakaoPlaceId: String? = null,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    @SerialName("kakao_place_url") val kakaoPlaceUrl: String? = null,
    @SerialName("naver_place_url") val naverPlaceUrl: String? = null,
    val phone: String? = null,
    @SerialName("business_hours") val businessHours: String? = null,
    @SerialName("instagram_url") val instagramUrl: String? = null,
    @SerialName("instagram_profile_image_path") val instagramProfileImagePath: String? = null,
    @SerialName("kakao_rating") val kakaoRating: Double? = null,
    @SerialName("menu_category_ids") val menuCategoryIds: List<String>? = null,
    @SerialName("is_visible") val isVisible: Boolean? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("like_count") val likeCount: Long,
) {
    fun toDomain(): ShopRanking =
        ShopRanking(
            shop =
                RamenShopResponse(
                    id = id,
                    kakaoPlaceId = kakaoPlaceId,
                    name = name,
                    address = address,
                    lat = lat,
                    lng = lng,
                    kakaoPlaceUrl = kakaoPlaceUrl,
                    naverPlaceUrl = naverPlaceUrl,
                    phone = phone,
                    businessHours = businessHours,
                    instagramUrl = instagramUrl,
                    instagramProfileImagePath = instagramProfileImagePath,
                    kakaoRating = kakaoRating,
                    menuCategoryIds = menuCategoryIds,
                    isVisible = isVisible,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                ).toDomain(),
            likeCount = likeCount,
        )

    fun toCursor(): RankingCursor =
        RankingCursor(
            likeCount = likeCount,
            name = name,
            shopId = id,
        )
}
