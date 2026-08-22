package com.peto.ramap.ui.main.event.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.report.InstagramNewsUrlParser
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsLoadKey
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.contract.mapEventsToUiState
import com.peto.ramap.ui.main.event.list.contract.selectEventFilter
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message
import ramap.shared.generated.resources.news_report_duplicate_message
import ramap.shared.generated.resources.news_report_failure_message
import ramap.shared.generated.resources.news_report_invalid_url_message
import ramap.shared.generated.resources.news_report_required_message
import ramap.shared.generated.resources.news_report_success_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val shopReportRepository: ShopReportRepository,
    private val eventsAnalytics: EventsAnalytics,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    init {
        loadEvents()
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.OnEventsRefreshed -> refreshEvents()
            EventsIntent.OnEventsRetried -> loadEvents()
            is EventsIntent.OnFilterSelected -> reduce { selectEventFilter(this, intent.filter) }
            is EventsIntent.OnEventClicked -> eventsAnalytics.logEventSelected(intent.event)
            EventsIntent.OnNewsReportClicked -> reduce { copy(showNewsReportDialog = true) }
            EventsIntent.OnNewsReportDismissed ->
                reduce { copy(showNewsReportDialog = false, newsReportContent = "", newsReportEvidence = null) }
            is EventsIntent.OnNewsReportContentChanged -> reduce { copy(newsReportContent = intent.value) }
            is EventsIntent.OnNewsReportEvidenceSelected -> reduce { copy(newsReportEvidence = intent.evidence) }
            EventsIntent.OnNewsReportEvidenceRemoved -> reduce { copy(newsReportEvidence = null) }
            EventsIntent.OnNewsReportSubmit -> submitNewsReport()
        }
    }

    private fun loadEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
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
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showError = false) },
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { mapEventsToUiState(this, events) } },
            onError = {
                showToast(
                    message = Res.string.event_list_refresh_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun submitNewsReport() {
        val content = currentState.newsReportContent
        val sourceUrl = InstagramNewsUrlParser.extractCanonicalUrl(content)
        if (content.isNotBlank() && sourceUrl == null) {
            showToast(Res.string.news_report_invalid_url_message)
            return
        }
        if (sourceUrl == null && currentState.newsReportEvidence == null) {
            showToast(Res.string.news_report_required_message)
            return
        }

        launchResultTask(
            taskKey = NEWS_REPORT_TASK_KEY,
            loadKey = EventsLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = {
                shopReportRepository.submitNewsReport(
                    NewsReport(sourceUrl, currentState.newsReportEvidence),
                )
            },
            onSuccess = { submission ->
                when (submission) {
                    NewsReportSubmission.SUBMITTED -> {
                        reduce {
                            copy(
                                showNewsReportDialog = false,
                                newsReportContent = "",
                                newsReportEvidence = null,
                            )
                        }
                        showToast(Res.string.news_report_success_message, ToastType.SUCCESS)
                    }
                    NewsReportSubmission.DUPLICATE -> showToast(Res.string.news_report_duplicate_message)
                }
            },
            onError = {
                showToast(Res.string.news_report_failure_message)
            },
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

    companion object {
        private const val EVENTS_TASK_KEY = "events"
        private const val NEWS_REPORT_TASK_KEY = "news-report"
    }
}
