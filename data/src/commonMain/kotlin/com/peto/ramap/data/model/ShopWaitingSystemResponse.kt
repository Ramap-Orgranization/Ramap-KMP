package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopWaitingSystemResponse(
    @SerialName("waiting_provider")
    val provider: String? = null,
    @SerialName("waiting_provider_url")
    val providerUrl: String? = null,
) {
    fun toDomain(shopId: String): WaitingSystem? =
        provider?.let {
            WaitingSystem(
                id = shopId,
                shopId = shopId,
                provider = WaitingProvider.fromId(it),
                providerUrl = providerUrl,
            )
        }
}
