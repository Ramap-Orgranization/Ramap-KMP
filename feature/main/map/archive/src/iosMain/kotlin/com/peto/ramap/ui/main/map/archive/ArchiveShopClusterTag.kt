package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.domain.model.shop.RamenShop
import platform.darwin.NSObject

internal class ArchiveShopClusterTag(
    val shops: List<RamenShop>,
) : NSObject()
