package com.peto.ramap.ui.main.ranking.contract

import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.ui.base.Intent

sealed interface RankingIntent : Intent {
    data object OnRefreshed : RankingIntent

    data object OnRetried : RankingIntent

    data object OnNextPageRequested : RankingIntent

    data object OnNextPageRetried : RankingIntent

    data object OnAllCategoriesSelected : RankingIntent

    data class OnAreaFilterSelected(
        val areaFilter: AreaFilter,
    ) : RankingIntent

    data class OnCategoryToggled(
        val category: Category,
    ) : RankingIntent

    data class OnBookmarkChanged(
        val shopId: String,
        val enabled: Boolean,
    ) : RankingIntent

    data object OnKakaoLoginClicked : RankingIntent
}
