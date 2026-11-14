package com.peto.ramap.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.BookmarkedFilterButton
import com.peto.ramap.designsystem.tab.AppChipTabs
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.NewsTab
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.list.EventsScreen
import com.peto.ramap.ui.main.event.list.EventsViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.preview.EventsPreviewParameterProvider
import com.peto.ramap.ui.main.newshops.NewShopsRoute
import com.peto.ramap.ui.main.notice.OperatingNoticeScreen
import com.peto.ramap.ui.main.notice.OperatingNoticeViewModel
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import com.peto.ramap.ui.main.notice.preview.OperatingNoticeUiStatePreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.news_tab_event
import ramap.shared.generated.resources.news_tab_new_shops
import ramap.shared.generated.resources.news_tab_operating_notice
import ramap.shared.generated.resources.operating_notice_saved

@Composable
fun NewsRoute(
    route: ScreenRoutes.EventTabRoutes,
    onClickEvent: (ShopEvent) -> Unit,
    onShopClick: (String) -> Unit,
    onNewShopClick: (String) -> Unit,
    navigationState: NavigationState,
    toastManager: ToastManager = koinInject(),
    eventsViewModel: EventsViewModel = koinViewModel(),
    noticeViewModel: OperatingNoticeViewModel = koinViewModel(),
) {
    val eventsUiState by eventsViewModel.uiState.collectAsStateWithLifecycle()
    val noticeUiState by noticeViewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(eventsViewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is EventsSideEffect.ShowEventsToast -> toastManager.show(sideEffect.data)
        }
    }
    ObserveAsEvents(noticeViewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is OperatingNoticeSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    NewsRouteContent(
        route = route,
        eventsUiState = eventsUiState,
        noticeUiState = noticeUiState,
        onClickEvent = onClickEvent,
        onShopClick = onShopClick,
        onNewShopClick = onNewShopClick,
        navigationState = navigationState,
        onEventsIntent = eventsViewModel::dispatch,
        onNoticeIntent = noticeViewModel::dispatch,
    )
}

@Composable
internal fun NewsRouteContent(
    route: ScreenRoutes.EventTabRoutes,
    eventsUiState: EventsUiState,
    noticeUiState: OperatingNoticeUiState,
    onClickEvent: (ShopEvent) -> Unit,
    onShopClick: (String) -> Unit,
    onNewShopClick: (String) -> Unit,
    navigationState: NavigationState,
    onEventsIntent: (EventsIntent) -> Unit = {},
    onNoticeIntent: (OperatingNoticeIntent) -> Unit = {},
) {
    var noticeOpened by rememberSaveable(route.instanceId) { mutableStateOf(false) }

    val activeTab = navigationState.selectedNewsTab

    @Composable
    fun Tabs(
        savedOnly: Boolean = false,
        onSavedToggled: (() -> Unit)? = null,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppChipTabs(
                labels =
                    NewsTab.entries.map { tab ->
                        stringResource(
                            when (tab) {
                                NewsTab.EVENTS -> Res.string.news_tab_event
                                NewsTab.OPERATING_NOTICES -> Res.string.news_tab_operating_notice
                                NewsTab.NEW_SHOPS -> Res.string.news_tab_new_shops
                            },
                        )
                    },
                selectedIndex = activeTab.ordinal,
                onSelect = { index -> navigationState.selectNewsTab(NewsTab.entries[index]) },
                modifier = Modifier.weight(1f),
            )
            if (onSavedToggled != null) {
                BookmarkedFilterButton(
                    isActive = savedOnly,
                    onClick = onSavedToggled,
                    contentDescription = stringResource(Res.string.operating_notice_saved),
                )
            }
        }
    }

    NewsScreen(
        selectedTab = activeTab,
        eventsContent = {
            EventsScreen(
                uiState = eventsUiState,
                onClickEvent = { event ->
                    onEventsIntent(EventsIntent.OnEventClicked(event))
                    onClickEvent(event)
                },
                onRefresh = { onEventsIntent(EventsIntent.OnEventsRefreshed) },
                onClickRetry = { onEventsIntent(EventsIntent.OnEventsRetried) },
                onPersonalizationRetry = { onEventsIntent(EventsIntent.OnPersonalizationRetried) },
                headerContent = {
                    Tabs(
                        savedOnly = eventsUiState.savedOnly,
                        onSavedToggled = { onEventsIntent(EventsIntent.OnSavedToggled) },
                    )
                },
            )
        },
        operatingNoticesContent = {
            OperatingNoticeScreen(
                uiState = noticeUiState,
                initialNoticeId = route.noticeId?.takeUnless { noticeOpened },
                onInitialNoticeOpened = { noticeOpened = true },
                onRefresh = { onNoticeIntent(OperatingNoticeIntent.OnRefreshed) },
                onRetry = { onNoticeIntent(OperatingNoticeIntent.OnRetried) },
                onShopClick = onShopClick,
                onPersonalizationRetry = { onNoticeIntent(OperatingNoticeIntent.OnPersonalizationRetried) },
                onNoticeClick = { notice ->
                    onNoticeIntent(OperatingNoticeIntent.OnNoticeClicked(notice))
                },
                isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
                onSourceClick = ExternalUriOpener::open,
                headerContent = {
                    Tabs(
                        savedOnly = noticeUiState.savedOnly,
                        onSavedToggled = { onNoticeIntent(OperatingNoticeIntent.OnSavedToggled) },
                    )
                },
            )
        },
        newShopsContent = {
            NewShopsRoute(
                tabs = { Tabs() },
                onShopClick = onNewShopClick,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NewsRoutePreview() {
    RamapTheme {
        NewsRouteContent(
            route = ScreenRoutes.EventTabRoutes(),
            eventsUiState = EventsPreviewParameterProvider().values.first(),
            noticeUiState = OperatingNoticeUiStatePreviewParameterProvider.defaultState,
            onClickEvent = {},
            onShopClick = {},
            onNewShopClick = {},
            navigationState = rememberNavigationState(),
        )
    }
}
