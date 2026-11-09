package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountBookmarkRequest(
    @SerialName("user_id")
    val accountId: String,
    @SerialName("shop_id")
    val shopId: String,
)
