package com.peto.ramap.domain.model.event

import com.peto.ramap.domain.model.shop.RamenShop
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
    val venueShop: RamenShop,
    val collaboratorShops: List<RamenShop> = emptyList(),
    val externalParticipants: List<ExternalParticipant> = emptyList(),
    val waitingMethod: String?,
    val waitingUrl: String?,
    val activeEventCount: Int = 1,
    val collaborationPartnerCount: Int? = null,
    val cancelledDates: List<LocalDate> = emptyList(),
    val isCancelledToday: Boolean = false,
) {
    val venueShopId: String
        get() = venueShop.id

    val venueShopName: String
        get() = venueShop.name

    val venueProfileImageUrl: String?
        get() = venueShop.instagramProfileImageUrl

    fun occursOn(date: LocalDate): Boolean {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return false
        val end = runCatching { LocalDate.parse(endDate ?: startDate) }.getOrNull() ?: return false
        return date in start..end
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
                collaboratorShops.singleOrNull()?.name
            } else {
                venueShop.name
            }
        }
}
