package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminManagedEvent(
    val id: String,
    val title: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("shop_name") val shopName: String,
    @SerialName("cancelled_dates") val cancelledDates: List<String> = emptyList(),
    @SerialName("sold_out_dates") val soldOutDates: List<String> = emptyList(),
)
