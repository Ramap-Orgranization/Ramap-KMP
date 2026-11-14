package com.peto.ramap.ui.main.event.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.peto.ramap.designsystem.button.BookmarkedFilterButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.list.component.EventListBottomSheet
import com.peto.ramap.ui.main.event.list.component.EventListSkeleton
import com.peto.ramap.ui.main.event.list.component.eventSection
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.preview.EventsPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_filter_summer_limited
import ramap.shared.generated.resources.event_list_empty
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.event_list_ongoing_section
import ramap.shared.generated.resources.event_list_saved_empty
import ramap.shared.generated.resources.event_list_upcoming_section
import ramap.shared.generated.resources.news_load_error_illustration
import ramap.shared.generated.resources.operating_notice_saved

@Composable
fun EventsRoute(
    onClickEvent: (ShopEvent) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: EventsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is EventsSideEffect.ShowEventsToast -> toastManager.show(sideEffect.data)
        }
    }
    EventsScreen(
        uiState = uiState,
        onClickEvent = { event ->
            viewModel.dispatch(EventsIntent.OnEventClicked(event))
            onClickEvent(event)
        },
        onRefresh = { viewModel.dispatch(EventsIntent.OnEventsRefreshed) },
        onClickRetry = { viewModel.dispatch(EventsIntent.OnEventsRetried) },
        onSavedToggled = { viewModel.dispatch(EventsIntent.OnSavedToggled) },
        onPersonalizationRetry = { viewModel.dispatch(EventsIntent.OnPersonalizationRetried) },
    )
}

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onClickEvent: (ShopEvent) -> Unit,
    onRefresh: () -> Unit,
    onClickRetry: () -> Unit,
    onSavedToggled: (() -> Unit)? = null,
    onPersonalizationRetry: () -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
) {
    var selectedEventGroup by remember { mutableStateOf<ShopEvents?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    val showHeaderAboveContent =
        uiState.isLoading ||
            uiState.showError ||
            uiState.savedFilterError ||
            (uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Loading)
    Column(modifier = Modifier.fillMaxSize()) {
        if (onSavedToggled != null && headerContent == null) {
            BookmarkedFilterButton(
                isActive = uiState.savedOnly,
                onClick = onSavedToggled,
                modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp, vertical = 8.dp),
                contentDescription = stringResource(Res.string.operating_notice_saved),
            )
        }
        if (showHeaderAboveContent) headerContent?.invoke()
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

                    uiState.savedOnly && uiState.personalizationState == PersonalizationBootstrapState.Loading ->
                        EventListSkeleton(modifier = Modifier.fillMaxSize())

                    uiState.savedFilterError ->
                        LoadErrorContent(
                            image = Res.drawable.news_load_error_illustration,
                            title = stringResource(Res.string.event_list_error_title),
                            description = stringResource(Res.string.event_list_error_description),
                            onRetry = onPersonalizationRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    uiState.showError ->
                        LoadErrorContent(
                            image = Res.drawable.news_load_error_illustration,
                            title = stringResource(Res.string.event_list_error_title),
                            description = stringResource(Res.string.event_list_error_description),
                            onRetry = onClickRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    else -> {
                        val summerLimitedTitle =
                            stringResource(Res.string.event_filter_summer_limited)
                        val ongoingTitle = stringResource(Res.string.event_list_ongoing_section)
                        val upcomingTitle = stringResource(Res.string.event_list_upcoming_section)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            headerContent?.let { header ->
                                item(key = "header") {
                                    header()
                                }
                            }
                            if (uiState.isEmpty) {
                                item {
                                    val emptyMessage =
                                        if (uiState.savedOnly) Res.string.event_list_saved_empty else Res.string.event_list_empty
                                    ShopsEmptyContent(
                                        title = stringResource(emptyMessage),
                                        titleStyle = AppTextStyle.T1,
                                    )
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
                                    useHorizontalScroll = false,
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
            onClickEvent = {},
            onRefresh = {},
            onClickRetry = {},
        )
    }
}
