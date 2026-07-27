package com.peto.ramap.ui.main.ranking.contract

import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.Intent

sealed interface RankingIntent : Intent {
    data object OnRefreshed : RankingIntent

    data object OnRetried : RankingIntent

    data object OnNextPageRequested : RankingIntent

    data object OnNextPageRetried : RankingIntent

    data object OnAllCategoriesSelected : RankingIntent

    data object OnAreaSheetOpened : RankingIntent

    data class OnAreaFilterSelected(
        val areaFilter: AreaFilter,
    ) : RankingIntent

    data class OnAdministrativeAreaSelected(
        val area: AdministrativeArea,
    ) : RankingIntent

    data object OnAreaSelectionBack : RankingIntent

    data class OnBookmarkChanged(
        val shop: RamenShop,
        val enabled: Boolean,
    ) : RankingIntent

    data class OnCategoryToggled(
        val category: Category,
    ) : RankingIntent

    data class OnShopClicked(
        val shop: RamenShop,
    ) : RankingIntent

    data object OnKakaoLoginClicked : RankingIntent
}
