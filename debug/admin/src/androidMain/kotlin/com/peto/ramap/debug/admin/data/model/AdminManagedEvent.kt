package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
internal data class AdminManagedEvent(
    val id: String,
    val title: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("shop_name") val shopName: String,
    val description: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("cancelled_dates") val cancelledDates: List<String> = emptyList(),
    @SerialName("sold_out_dates") val soldOutDates: List<String> = emptyList(),
) {
    val effectiveEndDate: String?
        get() =
            when (eventType) {
                "store_renewal" ->
                    LocalDate
                        .parse(startDate)
                        .plusMonths(1)
                        .minusDays(1)
                        .toString()
                "new_menu" -> LocalDate.parse(startDate).plusDays(6).toString()
                else -> endDate
            }

    fun isManageableOn(today: LocalDate): Boolean = runCatching { effectiveEndDate?.let { LocalDate.parse(it) >= today } ?: true }.getOrDefault(false)

    fun occursOn(today: LocalDate): Boolean = isManageableOn(today) && runCatching { LocalDate.parse(startDate) <= today }.getOrDefault(false)

    fun canChangeStatusOn(today: LocalDate): Boolean = eventType !in NON_STATUS_EVENT_TYPES && occursOn(today)

    fun isCancelledOn(today: LocalDate): Boolean = occursOn(today) && today.toString() in cancelledDates

    fun isSoldOutOn(today: LocalDate): Boolean = occursOn(today) && today.toString() in soldOutDates

    private companion object {
        val NON_STATUS_EVENT_TYPES = setOf("new_menu", "store_renewal")
    }
}
