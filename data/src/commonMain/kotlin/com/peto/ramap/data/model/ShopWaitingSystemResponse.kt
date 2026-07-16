package com.peto.ramap.data.model

import com.peto.ramap.domain.model.ShopWaitingSystem
import com.peto.ramap.domain.model.WaitingProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopWaitingSystemResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    val provider: String,
    @SerialName("provider_url")
    val providerUrl: String? = null,
) {
    fun toDomain(): ShopWaitingSystem =
        ShopWaitingSystem(
            id = id,
            shopId = shopId,
            provider = WaitingProvider.fromId(provider),
            providerUrl = providerUrl,
        )
}
