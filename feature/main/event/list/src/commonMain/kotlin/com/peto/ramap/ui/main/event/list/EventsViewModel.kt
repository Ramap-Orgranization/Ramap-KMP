package com.peto.ramap.ui.main.event.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SignInUseCase
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
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.event_list_refresh_failure_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.login_success_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val eventsAnalytics: EventsAnalytics,
    private val personalizationStore: PersonalizationStore,
    private val loginRepository: LoginRepository,
    private val signInUseCase: SignInUseCase,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    private var pendingSavedFilter = false
    private var signInSucceeded = false
    private var sessionGeneration = 0L

    init {
        observePersonalization()
        loadEvents()
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.OnLoginDismissed -> dismissLogin()
            is EventsIntent.OnLoginSelected -> signIn(intent.type)
            is EventsIntent.OnQueryChanged -> reduce { mapEventsToUiState(copy(query = intent.query)) }
            EventsIntent.OnSavedToggled -> toggleSaved()
            EventsIntent.OnPersonalizationRetried -> refreshPersonalization(resumeSaved = currentState.savedFilterError)
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
                            bookmarkedShopIds = if (loginRepository.hasSession()) value?.bookmarkedShopIds.orEmpty() else emptySet(),
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            loginRepository.session.collect { session ->
                if (session == LoginSessionState.AUTHENTICATED) {
                    resumeSavedFilter()
                } else {
                    sessionGeneration += 1
                    pendingSavedFilter = false
                    signInSucceeded = false
                    cancelTask(PERSONALIZATION_TASK_KEY)
                    cancelTask(SIGN_IN_TASK_KEY)
                    reduce { mapEventsToUiState(copy(savedOnly = false, savedFilterError = false, showLoginGuide = false, bookmarkedShopIds = emptySet(), personalizationState = PersonalizationBootstrapState.Loading)) }
                }
            }
        }
    }

    private fun toggleSaved() {
        if (!loginRepository.hasSession()) {
            pendingSavedFilter = true
            reduce { copy(showLoginGuide = true) }
            return
        }
        pendingSavedFilter = false
        signInSucceeded = false
        reduce { mapEventsToUiState(copy(savedOnly = !savedOnly, savedFilterError = false)) }
    }

    private fun dismissLogin() {
        pendingSavedFilter = false
        signInSucceeded = false
        reduce { copy(showLoginGuide = false) }
    }

    private fun signIn(type: LoginType) {
        reduce { copy(showLoginGuide = false) }
        val generation = sessionGeneration
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            loadKey = EventsLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            request = { signInUseCase(type, AnalyticsSource.EVENT_LIST) },
            onSuccess = {
                if (generation == sessionGeneration && pendingSavedFilter) {
                    signInSucceeded = true
                    showToast(Res.string.login_success_message, ToastType.DEFAULT)
                    resumeSavedFilter()
                }
            },
            onError = {
                dismissLogin()
                showToast(if (type == LoginType.KAKAO) Res.string.kakao_login_failure_message else Res.string.apple_login_failure_message)
            },
        )
    }

    private fun resumeSavedFilter() {
        if (!pendingSavedFilter || !signInSucceeded || !loginRepository.hasSession()) return
        signInSucceeded = false
        refreshPersonalization(resumeSaved = true)
    }

    private fun refreshPersonalization(resumeSaved: Boolean = false) {
        if (resumeSaved) pendingSavedFilter = true
        val generation = sessionGeneration
        launchResultTask(
            taskKey = PERSONALIZATION_TASK_KEY,
            loadKey = EventsLoadKey.Personalization,
            policy = TaskPolicy.IgnoreNew,
            request = personalizationStore::refresh,
            onSuccess = {
                if (resumeSaved && pendingSavedFilter && generation == sessionGeneration && loginRepository.hasSession()) {
                    pendingSavedFilter = false
                    val fresh = personalizationStore.state.value as? PersonalizationBootstrapState.Success
                    if (fresh != null) reduce { mapEventsToUiState(copy(savedOnly = true, savedFilterError = false, bookmarkedShopIds = fresh.value.bookmarkedShopIds, personalizationState = fresh)) }
                }
            },
            onError = {
                val shouldResume = resumeSaved && pendingSavedFilter
                pendingSavedFilter = false
                if (shouldResume && generation == sessionGeneration && loginRepository.hasSession()) {
                    reduce { copy(savedFilterError = true) }
                }
            },
        )
    }

    companion object {
        private const val SIGN_IN_TASK_KEY = "newsSignIn"
        private const val PERSONALIZATION_TASK_KEY = "eventPersonalization"
        private const val EVENTS_TASK_KEY = "events"
    }
}
