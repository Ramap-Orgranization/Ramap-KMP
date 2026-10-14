package com.peto.ramap.data.model

import com.peto.ramap.domain.model.menu.Menu
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.model.menu.Price
import com.peto.ramap.domain.model.menu.ShopMenu
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopMenuResponse(
    @SerialName("menu_sections")
    val menuSections: List<MenuSectionResponse>,
    @SerialName("menu_items")
    val menuItems: List<MenuResponse>,
    @SerialName("menu_updated_at")
    val menuUpdatedAt: String,
) {
    fun toDomain(): ShopMenu =
        ShopMenu(
            sections = menuSections(menuSections, menuItems),
            updatedAt = menuUpdatedAt,
        )

    private fun menuSections(
        sections: List<MenuSectionResponse>,
        items: List<MenuResponse>,
    ): List<MenuSection> {
        val itemsBySection = items.groupBy { it.sectionId }
        return sections
            .sortedWith(
                compareBy(::menuSectionPriority)
                    .thenBy { it.displayOrder }
                    .thenBy { it.id },
            ).map { menuSection(it, itemsBySection[it.id].orEmpty()) }
            .filter { it.items.isNotEmpty() }
    }

    private fun menuSectionPriority(section: MenuSectionResponse): Int = if (section.title.trim() == PERMANENT_MENU_SECTION_TITLE) 1 else 0

    private fun menuSection(
        section: MenuSectionResponse,
        items: List<MenuResponse>,
    ): MenuSection =
        MenuSection(
            id = section.id,
            title = section.title,
            description = section.description,
            displayOrder = section.displayOrder,
            items = Menus(items.sortedBy { it.displayOrder }.map(::menuItem)),
        )

    private fun menuItem(item: MenuResponse): Menu =
        Menu(
            id = item.id,
            name = item.name,
            priceKrw = item.priceKrw?.let(::Price),
            priceText = item.priceText,
            description = item.description,
            imageUrl = item.imageUrl,
            sourceUrl = item.sourceUrl,
            displayOrder = item.displayOrder,
            isRepresentative = item.isRepresentative,
        )

    companion object {
        private const val PERMANENT_MENU_SECTION_TITLE = "상시메뉴"
    }
}
