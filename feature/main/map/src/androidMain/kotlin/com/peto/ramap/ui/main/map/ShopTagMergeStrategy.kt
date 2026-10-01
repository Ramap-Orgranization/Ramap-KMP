package com.peto.ramap.ui.main.map

import com.naver.maps.map.clustering.Cluster
import com.naver.maps.map.clustering.TagMergeStrategy
import com.peto.ramap.domain.model.shop.RamenShop

internal class ShopTagMergeStrategy : TagMergeStrategy {
    override fun mergeTag(cluster: Cluster): Any = mergeShopTags(cluster.children.map { it.tag })
}

internal fun mergeShopTags(tags: List<Any?>): List<RamenShop> =
    tags.flatMap { tag ->
        when (tag) {
            is RamenShop -> listOf(tag)
            is List<*> -> tag.filterIsInstance<RamenShop>()
            else -> emptyList()
        }
    }
