package com.peto.ramap.ui.main.map.model

/** 상세 상태가 도착하기 전에도 카메라 이동 대상과 소비할 요청을 식별한다. */
data class ShopFocusRequest(
    val shopId: String,
    val sequence: Long,
)
