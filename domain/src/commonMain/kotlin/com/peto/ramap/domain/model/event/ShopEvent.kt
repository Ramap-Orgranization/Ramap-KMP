package com.peto.ramap.domain.model.event

import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
) {
    val venueShopId: String
        get() = venueShop.id

    val venueShopName: String
        get() = venueShop.name

    val venueProfileImageUrl: String?
        get() = venueShop.instagramProfileImageUrl

    val displayEndDate: String?
        get() {
            if (type != ShopEventType.STORE_RENEWAL) return endDate
            val start = parseStartDate() ?: return endDate
            return renewalEndDate(start).toString()
        }

    fun occursOn(date: LocalDate): Boolean {
        val start = parseStartDate() ?: return false
        val end =
            if (type == ShopEventType.STORE_RENEWAL) {
                renewalEndDate(start)
            } else {
                runCatching { LocalDate.parse(endDate ?: startDate) }.getOrNull() ?: return false
            }
        return date in start..end
    }

    private fun parseStartDate(): LocalDate? = runCatching { LocalDate.parse(startDate) }.getOrNull()

    private fun renewalEndDate(start: LocalDate): LocalDate =
        start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

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
    }
}
