package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import kotlinx.datetime.LocalDate

data class OperatingNoticeFeed(
    val referenceDate: LocalDate,
    val items: List<OperatingNoticeFeedItem>,
) {
    fun districts(area: AdministrativeArea): List<String> =
        items
            .filter { it.area == area }
            .mapNotNull { it.sigungu }
            .distinct()
            .sorted()

    fun representatives(
        scheduled: Boolean,
        query: String = "",
        areaFilter: AreaFilter = AreaFilter.Nationwide,
        bookmarkedShopIds: Set<String>? = null,
    ): List<OperatingNotice> {
        if (scheduled) return scheduledGroups(query, areaFilter, bookmarkedShopIds).values.flatten().distinctBy { it.shop.id }
        val notices = filteredItems(query, areaFilter, bookmarkedShopIds).map { it.notice }.filter { it.appliesOn(referenceDate) }
        return representativesOn(referenceDate, notices)
    }

    fun scheduledGroups(
        query: String = "",
        areaFilter: AreaFilter = AreaFilter.Nationwide,
        bookmarkedShopIds: Set<String>? = null,
    ): Map<LocalDate, List<OperatingNotice>> {
        val filtered = filteredItems(query, areaFilter, bookmarkedShopIds)
        val groups = mutableMapOf<LocalDate, MutableList<OperatingNotice>>()
        for (item in filtered) {
            for (date in item.notice.scheduledDates(referenceDate)) {
                groups.getOrPut(date) { mutableListOf() }.add(item.notice)
            }
        }
        return groups.keys.sorted().associateWith { date ->
            val scheduledShopIds = groups.getValue(date).map { it.shop.id }.toSet()
            val applicable = filtered.map { it.notice }.filter { it.shop.id in scheduledShopIds && it.appliesOn(date) }
            representativesOn(date, applicable)
        }
    }

    fun noticesForShopOn(
        shopId: String,
        date: LocalDate,
    ): List<OperatingNotice> =
        items
            .map { it.notice }
            .filter { it.shop.id == shopId && it.appliesOn(date) }
            .distinctBy { it.id }
            .sortedWith(noticePriority(date))

    private fun representativesOn(
        date: LocalDate,
        notices: List<OperatingNotice>,
    ): List<OperatingNotice> =
        notices
            .groupBy { it.shop.id }
            .values
            .map { it.minWith(noticePriority(date)) }
            .sortedWith(compareBy<OperatingNotice> { it.shop.name }.thenBy { it.shop.id })

    private fun noticePriority(date: LocalDate): Comparator<OperatingNotice> =
        compareBy<OperatingNotice> { if (it.type == OperatingNoticeType.TEMPORARY_CLOSURE) 0 else 1 }
            .thenBy { it.closingAt(date) }
            .thenBy { if (it.type == OperatingNoticeType.EARLY_CLOSING) 0 else 1 }
            .thenBy { it.id }

    private fun filteredItems(
        query: String,
        areaFilter: AreaFilter,
        bookmarkedShopIds: Set<String>?,
    ): List<OperatingNoticeFeedItem> =
        items.filter { item ->
            item.notice.shop.name
                .contains(query.trim(), ignoreCase = true) &&
                (bookmarkedShopIds == null || item.notice.shop.id in bookmarkedShopIds) &&
                matchesArea(item, areaFilter)
        }

    private fun matchesArea(
        item: OperatingNoticeFeedItem,
        filter: AreaFilter,
    ): Boolean =
        when (filter) {
            AreaFilter.Nationwide -> true
            is AreaFilter.Province -> item.area == filter.area
            is AreaFilter.District -> item.area == filter.area && item.sigungu == filter.district.name
        }
}
