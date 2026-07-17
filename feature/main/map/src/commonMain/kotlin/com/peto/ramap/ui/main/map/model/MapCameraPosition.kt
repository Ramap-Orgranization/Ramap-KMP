package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.Location

data class MapCameraPosition(
    val center: Location,
    val zoom: Double,
)
