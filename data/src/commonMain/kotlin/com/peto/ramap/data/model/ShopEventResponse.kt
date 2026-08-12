package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import kotlinx.datetime.LocalDate
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
    @SerialName("venue_shop") val venueShop: RamenShopResponse? = null,
    @SerialName("collaborator_shops") val collaboratorShops: List<RamenShopResponse> = emptyList(),
    @SerialName("external_participants") val externalParticipants: List<ExternalParticipantResponse> = emptyList(),
    @SerialName("venue_shop_id") val venueShopId: String? = null,
    @SerialName("venue_shop_name") val venueShopName: String? = null,
    @SerialName("venue_address") val venueAddress: String? = null,
    @SerialName("venue_kakao_place_url") val venueKakaoPlaceUrl: String? = null,
    @SerialName("venue_naver_place_url") val venueNaverPlaceUrl: String? = null,
    @SerialName("venue_lat") val venueLatitude: Double? = null,
    @SerialName("venue_lng") val venueLongitude: Double? = null,
    @SerialName("collaborator_shop_id") val collaboratorShopId: String? = null,
    @SerialName("collaborator_name") val collaboratorName: String? = null,
    @SerialName("collaborator_instagram_url") val collaboratorInstagramUrl: String? = null,
    @SerialName("venue_profile_image_url") val venueProfileImagePath: String? = null,
    @SerialName("waiting_method") val waitingMethod: String? = null,
    @SerialName("waiting_url") val waitingUrl: String? = null,
    @SerialName("cancelled_dates") val cancelledDates: List<String> = emptyList(),
    @SerialName("is_cancelled_today") val isCancelledToday: Boolean = false,
) {
    fun toDomain(): ShopEvent? {
        val type = runCatching { ShopEventType.valueOf(eventType.uppercase()) }.getOrNull() ?: return null
        val venueShop = resolveVenueShop() ?: return null
        return ShopEvent(
            id = id,
            type = type,
            title = title,
            description = description,
            startDate = startDate,
            endDate = endDate,
            sourceUrl = sourceUrl,
            isToday = isToday,
            isVenue = isVenue,
            venueShop = venueShop.toDomain(),
            collaboratorShops = resolveCollaboratorShops().map(RamenShopResponse::toDomain),
            externalParticipants = resolveExternalParticipants().map(ExternalParticipantResponse::toDomain),
            waitingMethod = waitingMethod,
            waitingUrl = waitingUrl,
            cancelledDates = cancelledDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
            isCancelledToday = isCancelledToday,
        )
    }

    private fun resolveVenueShop(): RamenShopResponse? {
        venueShop?.let { return it }
        return RamenShopResponse(
            id = venueShopId ?: return null,
            name = venueShopName ?: return null,
            address = venueAddress ?: return null,
            lat = venueLatitude ?: return null,
            lng = venueLongitude ?: return null,
            kakaoPlaceUrl = venueKakaoPlaceUrl,
            naverPlaceUrl = venueNaverPlaceUrl,
            instagramProfileImagePath = venueProfileImagePath,
            createdAt = "",
            updatedAt = "",
        )
    }

    private fun resolveCollaboratorShops(): List<RamenShopResponse> {
        if (collaboratorShops.isNotEmpty()) return collaboratorShops
        val shopId = collaboratorShopId ?: return emptyList()
        return listOf(
            RamenShopResponse(
                id = shopId,
                name = collaboratorName.orEmpty(),
                address = "",
                lat = 0.0,
                lng = 0.0,
                createdAt = "",
                updatedAt = "",
            ),
        )
    }

    private fun resolveExternalParticipants(): List<ExternalParticipantResponse> {
        if (externalParticipants.isNotEmpty()) return externalParticipants
        if (collaboratorShopId != null) return emptyList()
        if (collaboratorName == null && collaboratorInstagramUrl == null) return emptyList()
        return listOf(
            ExternalParticipantResponse(
                name = collaboratorName,
                instagramUrl = collaboratorInstagramUrl,
            ),
        )
    }
}
