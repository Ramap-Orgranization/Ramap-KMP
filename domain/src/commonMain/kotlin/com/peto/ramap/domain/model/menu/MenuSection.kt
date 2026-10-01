package com.peto.ramap.domain.model.menu

data class MenuSection(
    val id: String,
    val title: String,
    val description: String? = null,
    val displayOrder: Int,
    val items: Menus,
)
