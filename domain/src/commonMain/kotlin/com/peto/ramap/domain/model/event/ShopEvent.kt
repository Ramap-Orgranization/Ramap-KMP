package com.peto.ramap.domain.model.event

import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

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
    val cancellationReason: String? = null,
    val cancellationSourceUrl: String? = null,
    val soldOutDates: List<LocalDate> = emptyList(),
    val isSoldOutToday: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val isStartDateToday: Boolean = false,
) {
    val venueShopId: String
        get() = venueShop.id

    val venueShopName: String
        get() = venueShop.name

    val venueProfileImageUrl: String?
        get() = venueShop.instagramProfileImageUrl

    fun limitedMenuDuration(): LimitedMenuDuration? {
        if (type != ShopEventType.LIMITED_MENU && type != ShopEventType.SUMMER_LIMITED) return null

        val start = LocalDate.parse(startDate)
        val end = endDate?.let { LocalDate.parse(it) } ?: return null
        if (end < start) return null

        return when (start.daysUntil(end) + 1) {
            1 -> LimitedMenuDuration.ONE_DAY
            in 2 until LONG_TERM_MINIMUM_DAYS -> LimitedMenuDuration.SHORT_TERM
            else -> LimitedMenuDuration.LONG_TERM
        }
    }

    fun occursOn(date: LocalDate): Boolean {
        val start = LocalDate.parse(startDate)
        if (type == ShopEventType.STORE_RENEWAL) return date == start
        val end = LocalDate.parse(endDate ?: startDate)
        return date in start..end
    }

    val displayImageUrls: List<String>
        get() = imageUrls.take(MAX_IMAGE_COUNT)

    fun isCancelledOn(date: LocalDate): Boolean = occursOn(date) && date in cancelledDates

    fun isSoldOutOn(date: LocalDate): Boolean = occursOn(date) && date in soldOutDates

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

    companion object {
        const val MAX_IMAGE_COUNT = 5
        private const val LONG_TERM_MINIMUM_DAYS = 7
    }
}
