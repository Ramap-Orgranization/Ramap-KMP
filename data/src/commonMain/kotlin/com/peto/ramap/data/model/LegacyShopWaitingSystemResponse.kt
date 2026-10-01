package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LegacyShopWaitingSystemResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    val provider: String,
    @SerialName("provider_url")
    val providerUrl: String? = null,
) {
    fun toDomain(): WaitingSystem =
        WaitingSystem(
            id = id,
            shopId = shopId,
            provider = WaitingProvider.fromId(provider),
            providerUrl = providerUrl,
        )
}
