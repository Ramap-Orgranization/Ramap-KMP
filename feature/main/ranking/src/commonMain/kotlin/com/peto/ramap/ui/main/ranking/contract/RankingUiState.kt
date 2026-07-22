package com.peto.ramap.ui.main.ranking.contract

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.rank.RankedShop
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

@Immutable
data class RankingUiState(
    val shops: RankedShops = RankedShops(ShopRankings(emptyList())),
    val nextCursor: RankingCursor? = null,
    val areaFilter: AreaFilter = AreaFilter.Nationwide,
    val selectedCategories: Set<Category> = emptySet(),
    val bookmarkedShopIds: Set<String> = emptySet(),
    val bookmarkUpdatingShopIds: Set<String> = emptySet(),
    val bookmarkLikeCountDeltas: Map<String, Long> = emptyMap(),
    val showError: Boolean = false,
    val showNextPageError: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : State,
    LoadableState<RankingUiState> {
    val isLoading: Boolean
        get() = loadState.isLoading(RankingLoadKey.FirstPage)

    val isRefreshing: Boolean
        get() = loadState.isLoading(RankingLoadKey.Refresh)

    val isLoadingNext: Boolean
        get() = loadState.isLoading(RankingLoadKey.NextPage)

    val hasNext: Boolean
        get() = nextCursor != null

    fun displayedLikeCount(item: RankedShop): Long {
        val shopId = item.ranking.shop.id
        val delta = bookmarkLikeCountDeltas[shopId] ?: 0L
        return (item.ranking.likeCount + delta).coerceAtLeast(0L)
    }

    override fun withLoadingState(loadState: LoadState): RankingUiState = copy(loadState = loadState)
}
