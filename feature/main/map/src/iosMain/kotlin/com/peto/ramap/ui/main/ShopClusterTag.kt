package com.peto.ramap.ui.main

import com.peto.ramap.domain.model.shop.RamenShop
import platform.darwin.NSObject

internal class ShopClusterTag(
    val shops: List<RamenShop>,
) : NSObject()
