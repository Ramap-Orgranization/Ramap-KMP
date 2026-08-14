package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
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
    ): Boolean =
        (!filter.hasCategoryFilter || menuCategories.any { it in filter }) &&
            (!filter.isOpenSelected || isOpenAt(currentDateTime))

    fun isOpenAt(currentDateTime: LocalDateTime): Boolean = businessHoursDetails?.isOpenAt(currentDateTime) == true

    fun businessHoursStatus(currentDateTime: LocalDateTime): BusinessHoursStatus? = businessHoursDetails?.statusAt(currentDateTime)
}
