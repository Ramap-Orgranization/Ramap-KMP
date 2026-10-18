package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.shop.ShopOverView

sealed interface ShopOverviewCacheLookup {
    data class Hit(
        val overview: ShopOverView,
    ) : ShopOverviewCacheLookup

    data object Miss : ShopOverviewCacheLookup
}
