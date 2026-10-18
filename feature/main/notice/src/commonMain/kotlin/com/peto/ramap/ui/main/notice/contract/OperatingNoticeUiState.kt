package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class OperatingNoticeUiState(
    val todayOperatingNotices: List<OperatingNotice> = emptyList(),
    val scheduledOperatingNotices: List<OperatingNotice> = emptyList(),
    val feed: OperatingNoticeFeed? = null,
    val scheduledSelected: Boolean = false,
    val query: String = "",
    val areaFilter: AreaFilter = AreaFilter.Nationwide,
    val savedOnly: Boolean = false,
    val bookmarkedShopIds: Set<String> = emptySet(),
    val personalizationState: PersonalizationBootstrapState = PersonalizationBootstrapState.Loading,
    val showOperatingNoticeError: Boolean = false,
    override val loadState: LoadState = LoadState(),
    val fallbackReferenceDate: LocalDate = Clock.System.todayIn(TimeZone.of("Asia/Seoul")),
) : State,
    LoadableState<OperatingNoticeUiState> {
    val isLoading: Boolean
        get() = loadState.isLoading(OperatingNoticeLoadKey.Fetch)

    val isRefreshing: Boolean
        get() = loadState.isLoading(OperatingNoticeLoadKey.Refresh)

    val hasOperatingNotices: Boolean
        get() = todayOperatingNotices.isNotEmpty() || scheduledOperatingNotices.isNotEmpty()

    val showCounts: Boolean
        get() = !savedOnly || personalizationState is PersonalizationBootstrapState.Success

    val todayCount: Int?
        get() = if (showCounts) todayShops.size else null

    val scheduledCount: Int?
        get() = if (showCounts) scheduledShops.size else null

    val referenceDate: LocalDate
        get() = noticeFeed.referenceDate

    val todayShops: List<OperatingNotice>
        get() = noticeFeed.representatives(false, query, areaFilter, if (savedOnly) bookmarkedShopIds else null)

    val scheduledShops: List<OperatingNotice>
        get() = scheduledGroups.values.flatten().distinctBy { it.shop.id }

    val scheduledGroups: Map<LocalDate, List<OperatingNotice>>
        get() = noticeFeed.scheduledGroups(query, areaFilter, if (savedOnly) bookmarkedShopIds else null)

    private val noticeFeed: OperatingNoticeFeed
        get() =
            feed ?: OperatingNoticeFeed(
                referenceDate = fallbackReferenceDate,
                items = (todayOperatingNotices + scheduledOperatingNotices).distinctBy { it.id }.map { OperatingNoticeFeedItem(it) },
            )

    fun selectNotice(
        notice: OperatingNotice,
        date: LocalDate,
    ): OperatingNoticeSelection = OperatingNoticeSelection(notice, date, noticeFeed.noticesForShopOn(notice.shop.id, date))

    override fun withLoadingState(loadState: LoadState): OperatingNoticeUiState = copy(loadState = loadState)
}
