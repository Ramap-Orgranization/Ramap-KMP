@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.ui.main.map.archive.ArchiveShopMarkerTag
import com.peto.ramap.ui.main.map.archive.nmaps.NMCCluster
import com.peto.ramap.ui.main.map.archive.nmaps.NMCNode
import com.peto.ramap.ui.main.map.archive.nmaps.NMCTagMergeStrategyProtocol
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

internal class ArchiveShopTagMergeStrategy :
    NSObject(),
    NMCTagMergeStrategyProtocol {
    override fun mergeTag(cluster: NMCCluster): NSObject =
        ArchiveShopClusterTag(
            cluster.children.filterIsInstance<NMCNode>().flatMap { child ->
                when (val tag = child.tag) {
                    is ArchiveShopMarkerTag -> listOf(tag.shop)
                    is ArchiveShopClusterTag -> tag.shops
                    else -> emptyList()
                }
            },
        )
}
