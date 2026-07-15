package com.peto.ramap.ui.main.event.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.core.base.ObserveAsEvents
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.main.component.LaduckLoadingContent
import com.peto.ramap.ui.main.component.LoadErrorContent
import com.peto.ramap.ui.main.event.list.contract.EventListIntent
import com.peto.ramap.ui.main.event.list.contract.EventListSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventListUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_empty
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.event_list_loading_message
import ramap.shared.generated.resources.event_list_ongoing_section
import ramap.shared.generated.resources.event_list_upcoming_section
import ramap.shared.generated.resources.laduck_error_crying

@Composable
fun EventListRoute(
    onEventClick: (ShopEvent) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: EventListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is EventListSideEffect.ShowEventListToast -> toastManager.show(sideEffect.data)
        }
    }
    LaunchedEffect(viewModel) { viewModel.dispatch(EventListIntent.OnEventListEntered) }
    EventListScreen(
        uiState = uiState,
        onEventClick = onEventClick,
        onRefresh = { viewModel.dispatch(EventListIntent.OnEventListRefreshed) },
        onRetryClick = { viewModel.dispatch(EventListIntent.OnEventListRetried) },
    )
}

@Composable
fun EventListScreen(
    uiState: EventListUiState,
    onEventClick: (ShopEvent) -> Unit,
    onRefresh: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Column(modifier = Modifier.fillMaxSize().background(CommonColor.White).statusBarsPadding()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter),
                    containerColor = CommonColor.White,
                    color = CommonColor.Black,
                )
            },
        ) {
            when (val state = uiState.eventsState) {
                LoadState.Idle, LoadState.Loading ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LaduckLoadingContent()
                        AppText(
                            text = stringResource(Res.string.event_list_loading_message),
                            style = AppTextStyle.T1,
                            color = GrayColor.C500,
                        )
                    }

                LoadState.Error ->
                    LoadErrorContent(
                        image = Res.drawable.laduck_error_crying,
                        title = stringResource(Res.string.event_list_error_title),
                        description = stringResource(Res.string.event_list_error_description),
                        onRetry = onRetryClick,
                        modifier = Modifier.fillMaxSize(),
                    )

                is LoadState.Content ->
                    if (state.data.isEmpty()) {
                        EventListEmptyContent()
                    } else {
                        val (ongoingEvents, upcomingEvents) = partitionBySchedule(state.data)
                        val ongoingTitle = stringResource(Res.string.event_list_ongoing_section)
                        val upcomingTitle = stringResource(Res.string.event_list_upcoming_section)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            eventSection(
                                scope = this,
                                title = ongoingTitle,
                                events = ongoingEvents,
                                isHorizontal = true,
                                onEventClick = onEventClick,
                            )
                            eventSection(
                                scope = this,
                                title = upcomingTitle,
                                events = upcomingEvents,
                                isHorizontal = false,
                                onEventClick = onEventClick,
                            )
                        }
                    }
            }
        }
    }
}

private fun eventSection(
    scope: LazyListScope,
    title: String,
    events: List<ShopEvent>,
    isHorizontal: Boolean,
    onEventClick: (ShopEvent) -> Unit,
) {
    if (events.isEmpty()) return

    scope.item(key = title) {
        AppText(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
    }
    if (isHorizontal) {
        scope.item(key = "$title-events") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events, key = ShopEvent::id) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        modifier = Modifier.width(280.dp),
                    )
                }
            }
        }
    } else {
        scope.items(events, key = ShopEvent::id) { event ->
            EventCard(event = event, onClick = { onEventClick(event) })
        }
    }
}

internal fun partitionBySchedule(events: List<ShopEvent>): Pair<List<ShopEvent>, List<ShopEvent>> = events.partition(ShopEvent::isToday)

@Composable
private fun EventListEmptyContent() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        AppText(
            text = stringResource(Res.string.event_list_empty),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
        )
    }
}
