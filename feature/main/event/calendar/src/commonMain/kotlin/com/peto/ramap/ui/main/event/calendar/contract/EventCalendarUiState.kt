package com.peto.ramap.ui.main.event.calendar.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.groupShopEventsByDate
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.main.event.calendar.model.CalendarDayEvents
import com.peto.ramap.ui.main.event.calendar.model.CalendarMonth
import kotlinx.datetime.LocalDate
import com.peto.ramap.ui.loading.LoadState as TaskLoadState

data class EventCalendarUiState(
    val month: CalendarMonth,
    val events: List<ShopEvent> = emptyList(),
    val showError: Boolean = false,
    override val loadState: TaskLoadState = TaskLoadState(),
    val hasPreviousMonthEvents: Boolean = false,
    val hasNextMonthEvents: Boolean = false,
    val notificationDates: List<LocalDate> = emptyList(),
) : LoadableState<EventCalendarUiState> {
    val eventDays: List<CalendarDayEvents>
        get() =
            groupShopEventsByDate(month.days(), events)
                .map { (date, dayEvents) -> CalendarDayEvents(date, dayEvents) }

    val isLoading: Boolean
        get() = loadState.isLoading(EventCalendarLoadKey.Fetch)

    val isRefreshing: Boolean
        get() = loadState.isLoading(EventCalendarLoadKey.Refresh)

    override fun withLoadingState(loadState: TaskLoadState): EventCalendarUiState = copy(loadState = loadState)
}
