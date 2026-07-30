package com.peto.ramap.domain.usecase

sealed interface ShopDetailCacheLookup {
    data class Hit(
        val detail: ShopDetail,
    ) : ShopDetailCacheLookup

    data object Miss : ShopDetailCacheLookup
}
