package com.peto.ramap.ui.review.search.contract

import com.peto.ramap.ui.loading.LoadKey

sealed interface ReviewShopSearchLoadKey : LoadKey {
    data object Search : ReviewShopSearchLoadKey

    data object RecentShops : ReviewShopSearchLoadKey
}
