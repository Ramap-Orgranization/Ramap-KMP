package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminExternalVenue(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("instagram_url") val instagramUrl: String? = null,
    @SerialName("naver_map_url") val naverMapUrl: String? = null,
    @SerialName("kakao_map_url") val kakaoMapUrl: String? = null,
)
