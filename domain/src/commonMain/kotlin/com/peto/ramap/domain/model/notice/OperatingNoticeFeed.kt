package com.peto.ramap.domain.model.notice

import kotlinx.datetime.LocalDate

data class OperatingNoticeFeed(
    val referenceDate: LocalDate,
    val items: List<OperatingNoticeFeedItem>,
) {
    fun representatives(
        scheduled: Boolean,
        bookmarkedShopIds: Set<String>? = null,
    ): List<OperatingNotice> {
        if (scheduled) return scheduledGroups(bookmarkedShopIds).values.flatten().distinctBy { it.shop.id }
        val notices = filteredItems(bookmarkedShopIds).map { it.notice }.filter { it.appliesOn(referenceDate) }
        return representativesOn(referenceDate, notices)
    }

    fun scheduledGroups(bookmarkedShopIds: Set<String>? = null): Map<LocalDate, List<OperatingNotice>> {
        val scheduled = scheduledNotices(bookmarkedShopIds)
        val groups = mutableMapOf<LocalDate, MutableList<OperatingNotice>>()
        for (notice in scheduled) {
            for (date in notice.scheduledDates(referenceDate)) {
                groups.getOrPut(date) { mutableListOf() }.add(notice)
            }
        }
        return groups.keys.sorted().associateWith { date ->
            val scheduledShopIds = groups.getValue(date).map { it.shop.id }.toSet()
            val applicable = scheduled.filter { it.shop.id in scheduledShopIds && it.appliesOn(date) }
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

    fun scheduledNoticesForShop(shopId: String): List<OperatingNotice> =
        scheduledNotices()
            .filter { it.shop.id == shopId && it.scheduledDates(referenceDate).isNotEmpty() }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<OperatingNotice> { it.scheduledDates(referenceDate).first() }
                    .thenComparator { first, second -> noticePriority(first.scheduledDates(referenceDate).first()).compare(first, second) },
            )

    private fun scheduledNotices(bookmarkedShopIds: Set<String>? = null): List<OperatingNotice> =
        filteredItems(bookmarkedShopIds)
            .map { it.notice }
            .filter { it.startDate > referenceDate }

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

    private fun filteredItems(bookmarkedShopIds: Set<String>?): List<OperatingNoticeFeedItem> {
        if (bookmarkedShopIds == null) return items
        return items.filter { it.notice.shop.id in bookmarkedShopIds }
    }
}
