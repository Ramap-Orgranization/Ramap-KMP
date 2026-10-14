package com.peto.ramap.domain.model.menu

data class ShopMenu(
    val sections: List<MenuSection>,
    val updatedAt: String? = null,
) {
    val itemCount: Int get() = sections.sumOf { it.items.size }
}
