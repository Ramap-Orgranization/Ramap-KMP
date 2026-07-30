package com.peto.ramap.navigation.deeplink

data class ShopLinkConfig(
    val baseUrl: String,
    val webHost: String,
    val customScheme: String = "ramap",
    val customHost: String = "shop",
)
