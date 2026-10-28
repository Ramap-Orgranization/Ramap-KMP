package com.peto.ramap.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TabStatus {
    MAP,
    RANKING,
    EVENT,

    @SerialName("COMMUNITY")
    FEED,
    MY,
}
