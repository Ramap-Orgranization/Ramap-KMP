package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.domain.model.shop.ShopVisitTimeWindow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

internal data class HoursFilterDialogUiState(
    val option: Option = Option.All,
    private val visitTime: LocalDateTime? = null,
) {
    constructor(selected: ShopHoursFilter) : this(
        option =
            when (selected) {
                ShopHoursFilter.All -> Option.All
                ShopHoursFilter.OpenNow -> Option.OpenNow
                is ShopHoursFilter.VisitTime -> Option.VisitTime
            },
        visitTime = (selected as? ShopHoursFilter.VisitTime)?.dateTime,
    )

    fun select(option: Option): HoursFilterDialogUiState = copy(option = option)

    fun visitDateTime(window: ShopVisitTimeWindow): LocalDateTime = window.resolve(visitTime).dateTime

    fun filter(window: ShopVisitTimeWindow): ShopHoursFilter =
        when (option) {
            Option.All -> ShopHoursFilter.All
            Option.OpenNow -> ShopHoursFilter.OpenNow
            Option.VisitTime -> window.resolve(visitTime)
        }

    fun selectDate(
        date: LocalDate,
        window: ShopVisitTimeWindow,
    ): HoursFilterDialogUiState = copy(visitTime = LocalDateTime(date, visitDateTime(window).time))

    fun selectTime(
        time: LocalTime,
        window: ShopVisitTimeWindow,
    ): HoursFilterDialogUiState = copy(visitTime = LocalDateTime(visitDateTime(window).date, time))

    enum class Option {
        All,
        OpenNow,
        VisitTime,
    }
}
