package com.peto.ramap.domain.model

data class RamenShops(
    private val shops: Map<String, RamenShop>,
) : Map<String, RamenShop> by shops {
    /**
     * 이미 화면에 렌더링된 가게 id를 제외하고 새로 렌더링해야 하는 가게 목록을 반환한다.
     */
    fun filterNotContainShops(renderedShopIds: Set<String>) =
        values.filterNot { shop ->
            shop.id in renderedShopIds
        }

    /**
     * [filter]에 포함된 메뉴 카테고리와 일치하는 가게만 남긴 [RamenShops]를 반환한다.
     */
    fun filterByCategory(filter: RamenShopFilter): RamenShops {
        if (filter.isEmpty()) return this
        return RamenShops(
            shops.filterValues { shop ->
                shop.menuCategories.matches(filter)
            },
        )
    }

    /**
     * [shopIds]에 포함된 id를 가진 가게만 남긴 [RamenShops]를 반환한다.
     */
    fun filterByShopIds(shopIds: Set<String>): RamenShops =
        RamenShops(
            shops.filterKeys { shopId ->
                shopId in shopIds
            },
        )

    /**
     * 숨김 가게를 제외한 [RamenShops]를 반환한다.
     */
    fun filterNotHidden(hiddenShopIds: Set<String>): RamenShops {
        if (hiddenShopIds.isEmpty()) return this
        return RamenShops(
            shops.filterKeys { shopId ->
                shopId !in hiddenShopIds
            },
        )
    }

    /**
     * 현재 목록에 [bounds] 안에 포함되는 노출 가게가 하나라도 있는지 반환한다.
     */
    fun hasVisibleShopIn(bounds: MapBounds): Boolean = values.any { shop -> bounds.contains(shop.location) }

    /**
     * 가게를 숨김 상태로 표시한 [RamenShops]를 반환한다.
     */
    fun markHidden(hiddenShopIds: Set<String>): RamenShops {
        if (hiddenShopIds.isEmpty()) return this
        return RamenShops(
            mapValues { (shopId, shop) ->
                if (shopId in hiddenShopIds) {
                    shop.copy(isVisible = false)
                } else {
                    shop
                }
            },
        )
    }
}
