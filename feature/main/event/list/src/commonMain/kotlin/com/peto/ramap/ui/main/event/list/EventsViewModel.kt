package com.peto.ramap.ui.main.event.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsLoadKey
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.contract.mapEventsToUiState
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val eventsAnalytics: EventsAnalytics,
    private val personalizationStore: PersonalizationStore,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    init {
        observePersonalization()
        loadEvents()
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            is EventsIntent.OnQueryChanged -> reduce { mapEventsToUiState(copy(query = intent.query)) }
            EventsIntent.OnSavedToggled -> toggleSaved()
            EventsIntent.OnPersonalizationRetried -> refreshPersonalization()
            EventsIntent.OnEventsRefreshed -> refreshEvents()
            EventsIntent.OnEventsRetried -> loadEvents()
            is EventsIntent.OnEventClicked -> eventsAnalytics.logEventSelected(intent.event)
        }
    }

    private fun loadEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Fetch,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showError = false) },
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { mapEventsToUiState(this, events) } },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Refresh,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showError = false) },
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { mapEventsToUiState(this, events) } },
            onError = { showToast(message = Res.string.event_list_refresh_failure_message, type = ToastType.ERROR) },
        )
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        viewModelScope.launch {
            trySideEffect(EventsSideEffect.ShowEventsToast(ToastData(message, type)))
        }
    }

    private fun observePersonalization() {
        viewModelScope.launch {
            personalizationStore.state.collect { state ->
                val value = (state as? PersonalizationBootstrapState.Success)?.value
                reduce {
                    mapEventsToUiState(
                        copy(
                            personalizationState = state,
                            bookmarkedShopIds = value?.bookmarkedShopIds.orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    private fun toggleSaved() {
        reduce { mapEventsToUiState(copy(savedOnly = !savedOnly)) }
    }

    private fun refreshPersonalization() {
        launchResultTask(
            taskKey = PERSONALIZATION_TASK_KEY,
            loadKey = EventsLoadKey.Personalization,
            policy = TaskPolicy.IgnoreNew,
            request = personalizationStore::refresh,
        )
    }

    companion object {
        private const val PERSONALIZATION_TASK_KEY = "eventPersonalization"
        private const val EVENTS_TASK_KEY = "events"
    }
}
