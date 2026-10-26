package com.peto.ramap.domain.model.profile

import com.peto.ramap.domain.model.shop.RamenShop

data class PublicSavedShopsPage(
    val access: ProfileAccess,
    val shops: List<RamenShop>,
)
