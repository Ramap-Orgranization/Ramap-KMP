package com.peto.ramap.domain.model

data class ShopEvent(
    val id: String,
    val type: ShopEventType,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val sourceUrl: String,
    val isToday: Boolean,
    val isVenue: Boolean,
    val venueShopId: String,
    val venueShopName: String,
    val venueAddress: String,
    val collaboratorShopId: String?,
    val collaboratorName: String?,
    val collaboratorInstagramUrl: String?,
    val waitingMethod: String?,
    val waitingUrl: String?,
    val activeEventCount: Int = 1,
    val collaborationPartnerCount: Int? = null,
) {
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

    val formattedDate: String
        get() {
            val start = startDate.toKoreanDate()
            val end = endDate.toKoreanDate()
            return if (startDate == endDate) start else "$start $DATE_SEPARATOR $end"
        }

    companion object {
        private const val DATE_SEPARATOR = "~"
    }
}

private fun String.toKoreanDate(): String {
    val parts = split('-')
    if (parts.size != 3) return this
    return "${parts[0]}년 ${parts[1].toIntOrNull() ?: return this}월 ${parts[2].toIntOrNull() ?: return this}일"
}
