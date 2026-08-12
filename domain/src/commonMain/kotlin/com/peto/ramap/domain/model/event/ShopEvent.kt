package com.peto.ramap.domain.model.event

import kotlinx.datetime.LocalDate

data class ShopEvent(
    val id: String,
    val type: ShopEventType,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String?,
    val sourceUrl: String,
    val isToday: Boolean,
    val isVenue: Boolean,
    val venueShopId: String,
    val venueShopName: String,
    val venueAddress: String,
    val venueKakaoPlaceUrl: String? = null,
    val venueNaverPlaceUrl: String? = null,
    val venueLatitude: Double? = null,
    val venueLongitude: Double? = null,
    val collaboratorShopId: String?,
    val collaboratorName: String?,
    val collaboratorInstagramUrl: String?,
    val waitingMethod: String?,
    val waitingUrl: String?,
    val venueProfileImageUrl: String? = null,
    val activeEventCount: Int = 1,
    val collaborationPartnerCount: Int? = null,
    val cancelledDates: List<LocalDate> = emptyList(),
    val isCancelledToday: Boolean = false,
) {
    fun occursOn(date: LocalDate): Boolean {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return false
        val end = runCatching { LocalDate.parse(endDate ?: startDate) }.getOrNull() ?: return false
        return start <= date && date <= end
    }

    fun isCancelledOn(date: LocalDate): Boolean = occursOn(date) && date in cancelledDates

    val upcomingCollaborationPartnerName: String?
        get() {
            if (
                type != ShopEventType.COLLAB ||
                isToday ||
                activeEventCount != 1 ||
                collaborationPartnerCount != 1
            ) {
                return null
            }
            return if (isVenue) {
                collaboratorName?.takeIf { collaboratorShopId != null }
            } else {
                venueShopName
            }
        }
}
