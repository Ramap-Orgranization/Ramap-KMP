package com.peto.ramap.navigation.deeplink

sealed interface ShopDeepLink {
    data class Shop(
        val shopId: String,
        val reviewId: String? = null,
    ) : ShopDeepLink
}
