package com.peto.ramap.domain.model.shop

data class WaitingSystem(
    val id: String,
    val shopId: String,
    val provider: WaitingProvider,
    val providerUrl: String?,
)
