package com.peto.ramap.ui.main.event.calendar.contract

import com.peto.ramap.ui.base.Intent

sealed interface EventCalendarIntent : Intent {
    data object OnPreviousMonthClicked : EventCalendarIntent

    data object OnNextMonthClicked : EventCalendarIntent

    data object OnRetryClicked : EventCalendarIntent

    data object OnRefreshClicked : EventCalendarIntent
}
