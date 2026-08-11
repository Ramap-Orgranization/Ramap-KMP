package com.peto.ramap.ui.main.event.calendar

import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarIntent
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarLoadKey
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarSideEffect
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarUiState
import com.peto.ramap.ui.main.event.calendar.model.CalendarMonth
import com.peto.ramap.ui.task.TaskPolicy

class EventCalendarViewModel(
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<EventCalendarUiState, EventCalendarIntent, EventCalendarSideEffect>(
        EventCalendarUiState(month = CalendarMonth.currentMonth()),
    ) {
    init {
        loadCalendarEvents()
    }

    override suspend fun handleIntent(intent: EventCalendarIntent) {
        when (intent) {
            EventCalendarIntent.OnPreviousMonthClicked ->
                if (currentState.hasPreviousMonthEvents) {
                    changeMonth(currentState.month.previous())
                }
            EventCalendarIntent.OnNextMonthClicked ->
                if (currentState.hasNextMonthEvents) {
                    changeMonth(currentState.month.next())
                }
            EventCalendarIntent.OnRetryClicked -> {
                ramenShopRepository.invalidateCalendarEventPage(currentState.month.firstDay().toString())
                loadCalendarEvents()
            }
            EventCalendarIntent.OnRefreshClicked -> {
                ramenShopRepository.invalidateCalendarEventPage(currentState.month.firstDay().toString())
                loadCalendarEvents(isRefresh = true)
            }
        }
    }

    private fun changeMonth(month: CalendarMonth) {
        reduce {
            copy(
                month = month,
                events = emptyList(),
                notificationDates = emptyList(),
                hasPreviousMonthEvents = false,
                hasNextMonthEvents = false,
                showError = false,
            )
        }
        loadCalendarEvents()
    }

    private fun loadCalendarEvents(isRefresh: Boolean = false) {
        val month = currentState.month
        launchResultTask(
            taskKey = CALENDAR_TASK_KEY,
            loadKey = if (isRefresh) EventCalendarLoadKey.Refresh else EventCalendarLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
            onStart = { if (isRefresh) this else copy(showError = false) },
            retryOnNetworkError = true,
            request = { ramenShopRepository.fetchCalendarEventPage(month.firstDay().toString()) },
            onSuccess = { page ->
                reduce {
                    copy(
                        events = page.events,
                        hasPreviousMonthEvents = page.hasPrevious,
                        hasNextMonthEvents = page.hasNext,
                        notificationDates = page.notificationDates,
                        showError = false,
                    )
                }
            },
            onError = {
                if (!isRefresh) {
                    reduce { copy(showError = true) }
                }
            },
        )
    }

    private companion object {
        private const val CALENDAR_TASK_KEY = "event-calendar"
    }
}
