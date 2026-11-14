package com.peto.ramap.domain.model.shop

data class NewShop(
    val id: String,
    val name: String,
    val opening: ShopOpening,
    val coverImageUrl: String?,
)
