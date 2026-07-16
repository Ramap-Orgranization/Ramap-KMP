package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopEventNotificationSubscriptionResponse(
    @SerialName("shop_id") val shopId: String,
)
