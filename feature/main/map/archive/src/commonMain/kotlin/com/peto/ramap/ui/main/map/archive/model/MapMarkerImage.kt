package com.peto.ramap.ui.main.map.archive.model

import androidx.compose.ui.graphics.ImageBitmap

data class MapMarkerImage(
    val bitmap: ImageBitmap,
    val anchorY: Float,
    val key: String,
)
