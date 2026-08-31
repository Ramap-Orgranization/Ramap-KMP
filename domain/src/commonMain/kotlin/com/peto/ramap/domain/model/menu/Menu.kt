package com.peto.ramap.domain.model.menu

data class Menu(
    val id: String,
    val name: String,
    val priceKrw: Price? = null,
    val priceText: String? = null,
    val description: String?,
    val imageUrl: String?,
    val sourceUrl: String? = null,
    val displayOrder: Int,
    val isRepresentative: Boolean = false,
)
