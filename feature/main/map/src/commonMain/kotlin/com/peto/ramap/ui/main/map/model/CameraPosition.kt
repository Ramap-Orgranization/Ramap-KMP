package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.Location

data class CameraPosition(
    val center: Location,
    val zoom: Double,
)
