package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopEventResponse(
    val id: String,
    @SerialName("event_type") val eventType: String,
    val title: String,
    val description: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String?,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("is_today") val isToday: Boolean,
    @SerialName("is_venue") val isVenue: Boolean,
    @SerialName("venue_shop_id") val venueShopId: String,
    @SerialName("venue_shop_name") val venueShopName: String,
    @SerialName("venue_address") val venueAddress: String,
    @SerialName("venue_kakao_place_url") val venueKakaoPlaceUrl: String? = null,
    @SerialName("venue_naver_place_url") val venueNaverPlaceUrl: String? = null,
    @SerialName("venue_lat") val venueLatitude: Double? = null,
    @SerialName("venue_lng") val venueLongitude: Double? = null,
    @SerialName("collaborator_shop_id") val collaboratorShopId: String? = null,
    @SerialName("collaborator_name") val collaboratorName: String? = null,
    @SerialName("collaborator_instagram_url") val collaboratorInstagramUrl: String? = null,
    @SerialName("waiting_method") val waitingMethod: String? = null,
    @SerialName("waiting_url") val waitingUrl: String? = null,
    @SerialName("venue_profile_image_url") val venueProfileImageUrl: String? = null,
) {
    fun toDomain(): ShopEvent? {
        val type = runCatching { ShopEventType.valueOf(eventType.uppercase()) }.getOrNull() ?: return null
        return ShopEvent(
            id,
            type,
            title,
            description,
            startDate,
            endDate,
            sourceUrl,
            isToday,
            isVenue,
            venueShopId,
            venueShopName,
            venueAddress,
            venueKakaoPlaceUrl,
            venueNaverPlaceUrl,
            venueLatitude,
            venueLongitude,
            collaboratorShopId,
            collaboratorName,
            collaboratorInstagramUrl,
            waitingMethod,
            waitingUrl,
            venueProfileImageUrl,
        )
    }
}
