@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map.archive

import co.touchlab.kermit.Logger
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.archive.ArchiveShopClusteringKey
import com.peto.ramap.ui.main.map.archive.ArchiveShopMarkerTag
import com.peto.ramap.ui.main.map.archive.markerUIImage
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapInteractionConfig
import com.peto.ramap.ui.main.map.archive.model.MapMarkerImage
import com.peto.ramap.ui.main.map.archive.nmaps.NMCDefaultLeafMarkerUpdater
import com.peto.ramap.ui.main.map.archive.nmaps.NMCLeafMarkerInfo
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMarker
import com.peto.ramap.ui.main.map.archive.nmaps.NMFOverlayImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPointMake

private val leafLogger = Logger.withTag("RamapArchiveIosLeafMarker")

internal class ArchiveShopLeafMarkerUpdater(
    private val onShopClick: (RamenShop) -> Unit,
) : NMCDefaultLeafMarkerUpdater() {
    private var markerImages: Map<String, MapMarkerImage> = emptyMap()

    fun updateImages(images: Map<String, MapMarkerImage>) {
        markerImages = images
    }

    override fun updateLeafMarker(
        info: NMCLeafMarkerInfo,
        _1: NMFMarker,
    ) {
        super.updateLeafMarker(info, _1)
        val source = info.tag ?: info.key
        leafLogger.d {
            "updateLeafMarker: source=${source::class.simpleName}, key=${info.key::class.simpleName}, tag=${info.tag?.let {
                it::class
                    .simpleName
            }}, position=${info.position.lat()},${info.position.lng()}"
        }
        val shop =
            when (source) {
                is ArchiveShopMarkerTag -> source.shop
                is ArchiveShopClusteringKey -> source.shop
                else -> return
            }
        val custom = markerImages[shop.id] ?: return
        val image = markerUIImage(custom.bitmap) ?: return
        _1.anchor = CGPointMake(0.5, custom.anchorY.toDouble())
        leafLogger.d {
            "updateLeafMarker shop: id=${shop.id}, name=${shop.name}, visible=${shop.isVisible}, markerBeforeAlpha=${_1.alpha}"
        }
        _1.iconImage =
            NMFOverlayImage.overlayImageWithImage(
                image = image,
                reuseIdentifier = "${shop.id}:${custom.key}",
            )
        _1.captionText = shop.name
        _1.alpha = if (shop.isVisible) 1.0 else ArchiveMapInteractionConfig.HIDDEN_SHOP_ALPHA.toDouble()
        _1.touchHandler = {
            onShopClick(shop)
            true
        }
        leafLogger.d {
            "updateLeafMarker applied: id=${shop.id}, caption=${_1.captionText}, alpha=${_1.alpha}"
        }
    }
}
