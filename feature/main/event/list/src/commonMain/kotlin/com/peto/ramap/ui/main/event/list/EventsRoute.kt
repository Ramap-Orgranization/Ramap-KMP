package com.peto.ramap.ui.main.event.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.list.component.EventFilters
import com.peto.ramap.ui.main.event.list.component.EventListBottomSheet
import com.peto.ramap.ui.main.event.list.component.EventListEmptyContent
import com.peto.ramap.ui.main.event.list.component.EventListSkeleton
import com.peto.ramap.ui.main.event.list.component.NewsReportDialog
import com.peto.ramap.ui.main.event.list.component.eventSection
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.platform.rememberNewsReportImagePicker
import com.peto.ramap.ui.main.event.list.preview.EventsPreviewParameterProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_filter_summer_limited
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.event_list_ongoing_section
import ramap.shared.generated.resources.event_list_upcoming_section
import ramap.shared.generated.resources.ic_operating_notice_fab
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.new_menu_ongoing_section
import ramap.shared.generated.resources.new_menu_upcoming_section
import ramap.shared.generated.resources.news_report_open
import ramap.shared.generated.resources.operating_notice_open

@Composable
fun EventsRoute(
    onClickEvent: (ShopEvent) -> Unit,
    onClickNotice: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: EventsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagePicker =
        rememberNewsReportImagePicker { evidence ->
            viewModel.dispatch(EventsIntent.OnNewsReportEvidenceSelected(evidence))
        }
    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is EventsSideEffect.ShowEventsToast -> toastManager.show(sideEffect.data)
        }
    }
    EventsScreen(
        uiState = uiState,
        onClickNotice = onClickNotice,
        onClickEvent = { event ->
            viewModel.dispatch(EventsIntent.OnEventClicked(event))
            onClickEvent(event)
        },
        onRefresh = { viewModel.dispatch(EventsIntent.OnEventsRefreshed) },
        onClickRetry = { viewModel.dispatch(EventsIntent.OnEventsRetried) },
        onFilterSelected = { filter -> viewModel.dispatch(EventsIntent.OnFilterSelected(filter)) },
        onClickNewsReport = { viewModel.dispatch(EventsIntent.OnNewsReportClicked) },
        onNewsReportContentChanged = { viewModel.dispatch(EventsIntent.OnNewsReportContentChanged(it)) },
        onNewsReportImagePick = imagePicker,
        onNewsReportEvidenceRemoved = { viewModel.dispatch(EventsIntent.OnNewsReportEvidenceRemoved) },
        onNewsReportSubmit = { viewModel.dispatch(EventsIntent.OnNewsReportSubmit) },
        onNewsReportDismiss = { viewModel.dispatch(EventsIntent.OnNewsReportDismissed) },
    )
}

@Composable
internal fun EventsScreen(
    uiState: EventsUiState,
    onClickNotice: () -> Unit,
    onClickEvent: (ShopEvent) -> Unit,
    onRefresh: () -> Unit,
    onClickRetry: () -> Unit,
    onFilterSelected: (EventFilter) -> Unit,
    onClickNewsReport: () -> Unit,
    onNewsReportContentChanged: (String) -> Unit,
    onNewsReportImagePick: () -> Unit,
    onNewsReportEvidenceRemoved: () -> Unit,
    onNewsReportSubmit: () -> Unit,
    onNewsReportDismiss: () -> Unit,
) {
    var selectedEventGroup by remember { mutableStateOf<ShopEvents?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .statusBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = CommonColor.White,
                        color = CommonColor.Black,
                    )
                },
            ) {
                when {
                    uiState.isLoading -> EventListSkeleton(modifier = Modifier.fillMaxSize())

                    uiState.showError ->
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.event_list_error_title),
                            description = stringResource(Res.string.event_list_error_description),
                            onRetry = onClickRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    else -> {
                        val summerLimitedTitle =
                            stringResource(Res.string.event_filter_summer_limited)
                        val ongoingTitleRes =
                            if (uiState.selectedFilter == EventFilter.NEW_MENU) {
                                Res.string.new_menu_ongoing_section
                            } else {
                                Res.string.event_list_ongoing_section
                            }
                        val upcomingTitleRes =
                            if (uiState.selectedFilter == EventFilter.NEW_MENU) {
                                Res.string.new_menu_upcoming_section
                            } else {
                                Res.string.event_list_upcoming_section
                            }

                        val ongoingTitle = stringResource(ongoingTitleRes)
                        val upcomingTitle = stringResource(upcomingTitleRes)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item {
                                EventFilters(
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = onFilterSelected,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }

                            if (uiState.isEmpty) {
                                item {
                                    EventListEmptyContent(modifier = Modifier.fillMaxSize())
                                }
                            } else {
                                eventSection(
                                    scope = this,
                                    title = summerLimitedTitle,
                                    events = uiState.summerLimitedEvents,
                                    isOngoingSection = true,
                                    useHorizontalScroll = uiState.upcomingEvents.isNotEmpty(),
                                    horizontalContentPadding = 5.dp,
                                    onEventClick = onClickEvent,
                                    onEventGroupClick = { selectedEventGroup = it },
                                )
                                eventSection(
                                    scope = this,
                                    title = ongoingTitle,
                                    events = uiState.ongoingEvents,
                                    isOngoingSection = true,
                                    useHorizontalScroll = uiState.upcomingEvents.isNotEmpty(),
                                    horizontalContentPadding = 5.dp,
                                    onEventClick = onClickEvent,
                                    onEventGroupClick = { selectedEventGroup = it },
                                )
                                eventSection(
                                    scope = this,
                                    title = upcomingTitle,
                                    events = uiState.upcomingEvents,
                                    isOngoingSection = false,
                                    horizontalContentPadding = 10.dp,
                                    onEventClick = onClickEvent,
                                    onEventGroupClick = { selectedEventGroup = it },
                                )
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = onClickNewsReport,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .offset(y = (-72).dp),
                shape = CircleShape,
                containerColor = CommonColor.White,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_report),
                    contentDescription = stringResource(Res.string.news_report_open),
                    modifier = Modifier.padding(3.dp),
                )
            }
            FloatingActionButton(
                onClick = onClickNotice,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 5.dp)
                        .padding(20.dp),
                shape = CircleShape,
                containerColor = CommonColor.White,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_operating_notice_fab),
                    contentDescription = stringResource(Res.string.operating_notice_open),
                    modifier = Modifier.padding(start = 3.dp),
                )
            }
            selectedEventGroup?.let { eventGroup ->
                EventListBottomSheet(
                    events = eventGroup,
                    onDismiss = { selectedEventGroup = null },
                    onEventClick = { event ->
                        selectedEventGroup = null
                        onClickEvent(event)
                    },
                )
            }
            NewsReportDialog(
                value = uiState.newsReportContent,
                evidence = uiState.newsReportEvidence,
                visible = uiState.showNewsReportDialog,
                isSubmitting = uiState.isSubmittingNewsReport,
                onValueChange = onNewsReportContentChanged,
                onImagePick = onNewsReportImagePick,
                onEvidenceRemove = onNewsReportEvidenceRemoved,
                onSubmit = onNewsReportSubmit,
                onDismiss = onNewsReportDismiss,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsRoutePreview(
    @PreviewParameter(EventsPreviewParameterProvider::class) uiState: EventsUiState,
) {
    RamapTheme {
        EventsScreen(
            uiState = uiState,
            onClickNotice = {},
            onClickEvent = {},
            onRefresh = {},
            onClickRetry = {},
            onFilterSelected = {},
            onClickNewsReport = {},
            onNewsReportContentChanged = {},
            onNewsReportImagePick = {},
            onNewsReportEvidenceRemoved = {},
            onNewsReportSubmit = {},
            onNewsReportDismiss = {},
        )
    }
}
