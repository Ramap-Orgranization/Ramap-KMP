package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.Location

/** 초기 GPS 이동과 구분하여 반복되는 장소 선택을 한 번씩 처리한다. */
data class PlaceFocusRequest(
    val location: Location,
    val sequence: Long,
    val zoom: Double,
)
