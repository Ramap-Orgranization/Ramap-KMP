package com.peto.ramap.domain.model.community

import com.peto.ramap.domain.model.shop.RamenShop

data class PublicSavedShopsPage(
    val access: ProfileAccess,
    val shops: List<RamenShop>,
)
