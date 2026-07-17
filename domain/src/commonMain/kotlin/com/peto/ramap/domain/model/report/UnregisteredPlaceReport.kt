package com.peto.ramap.domain.model.report

import com.peto.ramap.domain.model.shop.Location

data class UnregisteredPlaceReport(
    val placeUrl: String? = null,
    val location: Location? = null,
)
