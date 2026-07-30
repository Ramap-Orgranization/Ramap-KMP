package com.peto.ramap.ui.main.map

import com.peto.ramap.domain.model.shop.RamenShop
import platform.darwin.NSObject

internal class ShopMarkerTag(
    val shop: RamenShop,
) : NSObject()
