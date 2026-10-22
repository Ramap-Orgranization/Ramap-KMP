package com.peto.ramap.ui.main.notice

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeLoadKey
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.main.notice.log.OperatingNoticeAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message
import ramap.shared.generated.resources.login_required_message

class OperatingNoticeViewModel(
    private val operatingNoticeRepository: OperatingNoticeRepository,
    private val operatingNoticeAnalytics: OperatingNoticeAnalytics,
    private val personalizationStore: PersonalizationStore,
    private val loginRepository: LoginRepository,
) : BaseViewModel<OperatingNoticeUiState, OperatingNoticeIntent, OperatingNoticeSideEffect>(
        OperatingNoticeUiState(),
    ) {
    init {
        observePersonalization()
        loadOperatingNotices()
    }

    override suspend fun handleIntent(intent: OperatingNoticeIntent) {
        when (intent) {
            is OperatingNoticeIntent.OnNoticeClicked -> operatingNoticeAnalytics.logNoticeSelected(intent.notice)
            is OperatingNoticeIntent.OnTabSelected -> reduce { copy(tab = intent.tab) }
            is OperatingNoticeIntent.OnQueryChanged -> reduce { copy(query = intent.query) }
            OperatingNoticeIntent.OnSavedToggled -> toggleSaved()
            OperatingNoticeIntent.OnPersonalizationRetried -> refreshPersonalization()
            OperatingNoticeIntent.OnRefreshed -> refreshOperatingNotices()
            OperatingNoticeIntent.OnRetried -> loadOperatingNotices()
        }
    }

    private fun loadOperatingNotices() {
        launchResultTask(
            taskKey = OPERATING_NOTICES_TASK_KEY,
            loadKey = OperatingNoticeLoadKey.Fetch,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showOperatingNoticeError = false) },
            retryOnNetworkError = true,
            request = operatingNoticeRepository::fetchOperatingNoticeFeed,
            onSuccess = ::applyOperatingNotices,
            onError = { reduce { copy(showOperatingNoticeError = true) } },
        )
    }

    private fun refreshOperatingNotices() {
        launchResultTask(
            taskKey = OPERATING_NOTICES_TASK_KEY,
            loadKey = OperatingNoticeLoadKey.Refresh,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showOperatingNoticeError = false) },
            retryOnNetworkError = true,
            request = operatingNoticeRepository::fetchOperatingNoticeFeed,
            onSuccess = ::applyOperatingNotices,
            onError = {
                showToast(
                    message = Res.string.event_list_refresh_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        viewModelScope.launch {
            trySideEffect(OperatingNoticeSideEffect.ShowToast(ToastData(message, type)))
        }
    }

    private fun applyOperatingNotices(feed: OperatingNoticeFeed) {
        val now = LocalDateTime(feed.referenceDate, LocalTime(0, 0))
        val (today, scheduled) = feed.items.map { it.notice }.partition { it.isActiveAt(now) }
        reduce { copy(feed = feed, todayOperatingNotices = today, scheduledOperatingNotices = scheduled) }
    }

    private fun observePersonalization() {
        viewModelScope.launch {
            personalizationStore.state.collect { state ->
                val value = (state as? PersonalizationBootstrapState.Success)?.value
                reduce {
                    copy(
                        personalizationState = state,
                        bookmarkedShopIds = if (loginRepository.hasSession()) value?.bookmarkedShopIds.orEmpty() else emptySet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            loginRepository.session.collect { session ->
                if (session != LoginSessionState.AUTHENTICATED) {
                    reduce { copy(savedOnly = false, bookmarkedShopIds = emptySet(), personalizationState = PersonalizationBootstrapState.Loading) }
                }
            }
        }
    }

    private fun toggleSaved() {
        if (!loginRepository.hasSession()) {
            showToast(Res.string.login_required_message)
            return
        }
        reduce { copy(savedOnly = !savedOnly) }
    }

    private fun refreshPersonalization() {
        launchResultTask(
            taskKey = PERSONALIZATION_TASK_KEY,
            loadKey = OperatingNoticeLoadKey.Personalization,
            policy = TaskPolicy.IgnoreNew,
            request = personalizationStore::refresh,
        )
    }

    companion object {
        private const val OPERATING_NOTICES_TASK_KEY = "operatingNotices"
        private const val PERSONALIZATION_TASK_KEY = "noticePersonalization"
    }
}
