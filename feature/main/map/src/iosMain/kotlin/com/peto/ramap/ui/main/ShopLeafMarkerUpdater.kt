@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main

import co.touchlab.kermit.Logger
import cocoapods.NMapsMap.NMCDefaultLeafMarkerUpdater
import cocoapods.NMapsMap.NMCLeafMarkerInfo
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMFOverlayImage
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.ShopClusteringKey
import com.peto.ramap.ui.main.map.ShopMarkerTag
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage

private const val MARKER_IMAGE_NAME = "marker_ramen"
private val leafLogger = Logger.withTag("RamapIosLeafMarker")

internal class ShopLeafMarkerUpdater(
    private val onShopClick: (RamenShop) -> Unit,
) : NMCDefaultLeafMarkerUpdater() {
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
                is ShopMarkerTag -> source.shop
                is ShopClusteringKey -> source.shop
                else -> return
            }
        val image = UIImage.imageNamed(MARKER_IMAGE_NAME)
        leafLogger.d {
            "updateLeafMarker shop: id=${shop.id}, name=${shop.name}, visible=${shop.isVisible}, imageLoaded=${image != null}, markerBeforeAlpha=${_1.alpha}"
        }
        image?.let {
            _1.iconImage =
                NMFOverlayImage.overlayImageWithImage(
                    image = it,
                    reuseIdentifier = MARKER_IMAGE_NAME,
                )
        }
        _1.captionText = shop.name
        _1.alpha = if (shop.isVisible) 1.0 else MapInteractionConfig.HIDDEN_SHOP_ALPHA.toDouble()
        _1.touchHandler = {
            onShopClick(shop)
            true
        }
        leafLogger.d {
            "updateLeafMarker applied: id=${shop.id}, caption=${_1.captionText}, alpha=${_1.alpha}"
        }
    }
}
