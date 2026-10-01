package com.peto.ramap.domain.model.event

import com.peto.ramap.domain.model.shop.RamenShop

sealed interface EventVenue {
    data class Registered(
        val shop: RamenShop,
    ) : EventVenue

    data class External(
        val id: String?,
        val name: String,
        val address: String? = null,
        val imageUrl: String? = null,
        val instagramUrl: String? = null,
        val naverMapUrl: String? = null,
        val kakaoMapUrl: String? = null,
    ) : EventVenue
}
