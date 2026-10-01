@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main

import cocoapods.NMapsMap.NMCCluster
import cocoapods.NMapsMap.NMCNode
import cocoapods.NMapsMap.NMCTagMergeStrategyProtocol
import com.peto.ramap.ui.main.map.ShopMarkerTag
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

internal class ShopTagMergeStrategy :
    NSObject(),
    NMCTagMergeStrategyProtocol {
    override fun mergeTag(cluster: NMCCluster): NSObject =
        ShopClusterTag(
            cluster.children.filterIsInstance<NMCNode>().flatMap { child ->
                when (val tag = child.tag) {
                    is ShopMarkerTag -> listOf(tag.shop)
                    is ShopClusterTag -> tag.shops
                    else -> emptyList()
                }
            },
        )
}
