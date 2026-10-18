package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeShopResponse(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    @SerialName("kakao_place_url")
    val kakaoPlaceUrl: String? = null,
    @SerialName("naver_place_url")
    val naverPlaceUrl: String? = null,
    @SerialName("business_hours_weekly")
    val businessHoursWeekly: Map<String, BusinessHoursDayResponse>? = null,
    @SerialName("business_hours_break_times")
    val businessHoursBreakTimes: Map<String, List<BreakTimeResponse>>? = null,
    @SerialName("business_hours_last_orders")
    val businessHoursLastOrders: Map<String, List<String>>? = null,
    @SerialName("business_hours_notice")
    val businessHoursNotice: String? = null,
    @SerialName("business_hours_notice_type")
    val businessHoursNoticeType: String? = null,
    @SerialName("instagram_url")
    val instagramUrl: String? = null,
    @SerialName("instagram_profile_image_path")
    val instagramProfileImagePath: String? = null,
    @SerialName("menu_category_ids")
    val menuCategoryIds: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
) {
    fun toDomain() =
        RamenShopResponse(
            address = "",
            id = id,
            name = name,
            lat = lat,
            lng = lng,
            kakaoPlaceUrl = kakaoPlaceUrl,
            naverPlaceUrl = naverPlaceUrl,
            businessHoursWeekly = businessHoursWeekly,
            businessHoursBreakTimes = businessHoursBreakTimes,
            businessHoursLastOrders = businessHoursLastOrders,
            businessHoursNotice = businessHoursNotice,
            businessHoursNoticeType = businessHoursNoticeType,
            instagramUrl = instagramUrl,
            instagramProfileImagePath = instagramProfileImagePath,
            menuCategoryIds = menuCategoryIds,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ).toDomain()
}
