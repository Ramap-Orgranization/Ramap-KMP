package com.peto.ramap.ui.main.map.archive.model

import com.peto.ramap.domain.model.shop.Location

data class CameraPosition(
    val center: Location,
    val zoom: Double,
)
