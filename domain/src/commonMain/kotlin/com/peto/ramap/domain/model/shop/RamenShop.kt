package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.datetime.LocalDateTime

data class RamenShop(
    val id: String,
    val kakaoPlaceId: String?,
    val name: String,
    val address: String,
    val location: Location,
    val kakaoPlaceUrl: String?,
    val naverPlaceUrl: String? = null,
    val phone: String?,
    val instagramUrl: String?,
    val menuCategories: MenuCategories,
    val isVisible: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val instagramProfileImageUrl: String? = null,
    val businessHoursDetails: BusinessHours? = null,
) {
    val hasCategory: Boolean
        get() = menuCategories.hasCategory

    fun isOpened(
        filter: RamenShopFilter,
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): Boolean =
        (!filter.hasCategoryFilter || menuCategories.any { it in filter }) &&
            (!filter.isOpenSelected || isOpenAt(currentDateTime, operatingNotices))

    fun isOpenAt(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): Boolean {
        if (businessHoursDetails?.isOpenAt(currentDateTime) != true) return false

        return !hasOperatingNoticeBlockingOpening(currentDateTime, operatingNotices)
    }

    fun businessHoursStatus(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): BusinessHoursStatus? {
        val status = businessHoursDetails?.statusAt(currentDateTime) ?: return null
        return if (hasOperatingNoticeBlockingOpening(currentDateTime, operatingNotices)) {
            BusinessHoursStatus.Closed()
        } else {
            status
        }
    }

    private fun hasOperatingNoticeBlockingOpening(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice>,
    ): Boolean =
        operatingNotices
            .asSequence()
            .filter { it.shop.id == id && it.isActiveAt(currentDateTime) }
            .any { notice ->
                when (notice.type) {
                    OperatingNoticeType.TEMPORARY_CLOSURE -> true
                    OperatingNoticeType.EARLY_CLOSING ->
                        notice.endTime != null && currentDateTime.time >= notice.endTime
                    OperatingNoticeType.LATE_OPENING ->
                        notice.startTime != null && currentDateTime.time < notice.startTime
                    OperatingNoticeType.OPERATING_NOTICE -> false
                }
            }
}
