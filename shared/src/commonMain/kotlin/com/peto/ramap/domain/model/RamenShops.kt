package com.peto.ramap.domain.model

data class RamenShops(
    private val shops: Map<String, RamenShop>,
) : Map<String, RamenShop> by shops {
    fun filterNotContainShops(renderedShopIds: Set<String>) =
        values.filterNot { shop ->
            shop.id in renderedShopIds
        }

    fun filterByCategory(filter: RamenShopFilter): RamenShops {
        if (filter.isEmpty()) return this
        return RamenShops(
            shops.filterValues { shop ->
                shop.menuCategories.matches(filter)
            },
        )
    }

    fun filterByShopIds(shopIds: Set<String>): RamenShops =
        RamenShops(
            shops.filterKeys { shopId ->
                shopId in shopIds
            },
        )

    fun filterNotHidden(hiddenShopIds: Set<String>): RamenShops {
        if (hiddenShopIds.isEmpty()) return this
        return RamenShops(
            shops.filterKeys { shopId ->
                shopId !in hiddenShopIds
            },
        )
    }

    fun hasVisibleShopIn(bounds: MapBounds): Boolean = values.any { shop -> bounds.contains(shop.location) }
}
