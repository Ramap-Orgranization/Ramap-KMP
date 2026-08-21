package com.peto.ramap.ui.main.notice

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeLoadKey
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

class OperatingNoticeViewModel(
    private val operatingNoticeRepository: OperatingNoticeRepository,
) : BaseViewModel<OperatingNoticeUiState, OperatingNoticeIntent, OperatingNoticeSideEffect>(
        OperatingNoticeUiState(),
    ) {
    init {
        loadOperatingNotices()
    }

    override suspend fun handleIntent(intent: OperatingNoticeIntent) {
        when (intent) {
            OperatingNoticeIntent.OnRefreshed -> refreshOperatingNotices()
            OperatingNoticeIntent.OnRetried -> loadOperatingNotices()
        }
    }

    private fun loadOperatingNotices() {
        launchResultTask(
            taskKey = OPERATING_NOTICES_TASK_KEY,
            loadKey = OperatingNoticeLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showOperatingNoticeError = false) },
            retryOnNetworkError = true,
            request = operatingNoticeRepository::fetchCurrentOperatingNotices,
            onSuccess = { notices -> reduce { copy(operatingNotices = notices) } },
            onError = { reduce { copy(showOperatingNoticeError = true) } },
        )
    }

    private fun refreshOperatingNotices() {
        launchResultTask(
            taskKey = OPERATING_NOTICES_TASK_KEY,
            loadKey = OperatingNoticeLoadKey.Refresh,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showOperatingNoticeError = false) },
            retryOnNetworkError = true,
            request = operatingNoticeRepository::fetchCurrentOperatingNotices,
            onSuccess = { notices -> reduce { copy(operatingNotices = notices) } },
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

    companion object {
        private const val OPERATING_NOTICES_TASK_KEY = "operatingNotices"
    }
}
