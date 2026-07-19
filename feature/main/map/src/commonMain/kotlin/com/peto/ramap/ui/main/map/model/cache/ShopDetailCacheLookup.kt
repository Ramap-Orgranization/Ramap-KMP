package com.peto.ramap.ui.main.map.model.cache

import com.peto.ramap.ui.main.map.model.ShopDetail

sealed interface ShopDetailCacheLookup {
    data class Hit(
        val detail: ShopDetail,
    ) : ShopDetailCacheLookup

    data object Miss : ShopDetailCacheLookup
}
