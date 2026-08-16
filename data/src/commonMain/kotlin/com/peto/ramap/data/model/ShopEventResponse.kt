package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.network.config.RamapSecrets
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
    @SerialName("venue_shop") val venueShop: RamenShopResponse,
    @SerialName("collaborator_shops") val collaboratorShops: List<RamenShopResponse> = emptyList(),
    @SerialName("external_participants") val externalParticipants: List<ExternalParticipantResponse> = emptyList(),
    @SerialName("waiting_method") val waitingMethod: String? = null,
    @SerialName("waiting_url") val waitingUrl: String? = null,
    @SerialName("cancelled_dates") val cancelledDates: List<String> = emptyList(),
    @SerialName("is_cancelled_today") val isCancelledToday: Boolean = false,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("cancellation_source_url") val cancellationSourceUrl: String? = null,
    @SerialName("image_paths") val imagePaths: List<String> = emptyList(),
    @SerialName("sold_out_dates") val soldOutDates: List<String> = emptyList(),
    @SerialName("is_sold_out_today") val isSoldOutToday: Boolean = false,
) {
    fun toDomain(): ShopEvent? {
        val type = runCatching { ShopEventType.valueOf(eventType.uppercase()) }.getOrNull() ?: return null
        return ShopEvent(
            id = id,
            type = type,
            title = title,
            description = description,
            startDate = startDate,
            endDate = if (type == ShopEventType.STORE_RENEWAL) null else endDate,
            sourceUrl = sourceUrl,
            isToday = isToday,
            isVenue = isVenue,
            venueShop = venueShop.toDomain(),
            collaboratorShops = collaboratorShops.map(RamenShopResponse::toDomain),
            externalParticipants = externalParticipants.map(ExternalParticipantResponse::toDomain),
            waitingMethod = waitingMethod,
            waitingUrl = waitingUrl,
            cancelledDates = cancelledDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
            isCancelledToday = isCancelledToday,
            cancellationReason = cancellationReason,
            cancellationSourceUrl = cancellationSourceUrl,
            soldOutDates = soldOutDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
            isSoldOutToday = isSoldOutToday,
            imageUrls = imagePaths.mapNotNull(::toPublicEventImageUrl).take(ShopEvent.MAX_IMAGE_COUNT),
        )
    }

    private fun toPublicEventImageUrl(path: String): String? {
        val normalizedPath = path.trim()
        if (
            normalizedPath.isBlank() ||
            normalizedPath.startsWith('/') ||
            normalizedPath.contains("..") ||
            normalizedPath.contains('\\') ||
            normalizedPath.contains("://") ||
            normalizedPath.contains('?') ||
            normalizedPath.contains('#')
        ) {
            return null
        }
        val baseUrl = RamapSecrets.supabaseUrl.trimEnd('/')
        if (baseUrl.isBlank()) return null
        return "$baseUrl$STORAGE_PUBLIC_PATH$EVENT_IMAGE_BUCKET/$normalizedPath"
    }

    private companion object {
        const val EVENT_IMAGE_BUCKET = "event-images"
        const val STORAGE_PUBLIC_PATH = "/storage/v1/object/public/"
    }
}
