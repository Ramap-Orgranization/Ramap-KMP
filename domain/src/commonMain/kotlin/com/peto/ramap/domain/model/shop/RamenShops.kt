package com.peto.ramap.domain.model.shop

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class RamenShops(
    private val shops: Map<String, RamenShop>,
) : Map<String, RamenShop> by shops {
    constructor(shops: Iterable<RamenShop>) : this(shops.associateBy(RamenShop::id))

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
        if (!filter.hasCategoryFilter) return this
        return RamenShops(
            shops.filterValues { shop ->
                shop.menuCategories.matches(filter)
            },
        )
    }

    fun filterBy(filter: RamenShopFilter): RamenShops {
        if (filter.isEmpty()) return this
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return RamenShops(shops.filterValues { shop -> shop.isOpened(filter) })
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

    fun nearestTo(location: Location?): RamenShop? {
        if (location == null) return null

        return values.minByOrNull { shop ->
            location.distanceMetersTo(shop.location)
        }
    }

    fun nearestFirstTo(location: Location?): RamenShops {
        val nearestShop = nearestTo(location) ?: return this
        return RamenShops(listOf(nearestShop) + values.filterNot { shop -> shop.id == nearestShop.id })
    }

    fun remove(shopId: String): RamenShops = RamenShops(shops - shopId)

    fun singleShopOrNull(): RamenShop? = values.singleOrNull()

    fun containsAll(shopIds: Set<String>): Boolean = shops.keys.containsAll(shopIds)
}
