package com.peto.ramap.ui.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.NewsTab
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.list.EventsScreen
import com.peto.ramap.ui.main.event.list.EventsViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.notice.OperatingNoticeScreen
import com.peto.ramap.ui.main.notice.OperatingNoticeViewModel
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewsRoute(
    route: ScreenRoutes.EventTabRoutes,
    onClickEvent: (ShopEvent) -> Unit,
    onShopClick: (String) -> Unit,
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

    var noticeOpened by rememberSaveable(route.instanceId) { mutableStateOf(false) }

    val activeTab = navigationState.selectedNewsTab

    NewsScreen(
        selectedTab = activeTab,
        onTabSelected = navigationState::selectNewsTab,
        query =
            when (activeTab) {
                NewsTab.EVENTS -> eventsUiState.query
                NewsTab.OPERATING_NOTICES -> noticeUiState.query
            },
        savedOnly =
            when (activeTab) {
                NewsTab.EVENTS -> eventsUiState.savedOnly
                NewsTab.OPERATING_NOTICES -> noticeUiState.savedOnly
            },
        onQueryChanged = { query ->
            when (activeTab) {
                NewsTab.EVENTS -> eventsViewModel.dispatch(EventsIntent.OnQueryChanged(query))
                NewsTab.OPERATING_NOTICES -> noticeViewModel.dispatch(OperatingNoticeIntent.OnQueryChanged(query))
            }
        },
        onSavedToggled = {
            when (activeTab) {
                NewsTab.EVENTS -> eventsViewModel.dispatch(EventsIntent.OnSavedToggled)
                NewsTab.OPERATING_NOTICES -> noticeViewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            }
        },
        eventsContent = {
            EventsScreen(
                uiState = eventsUiState,
                onClickEvent = { event ->
                    eventsViewModel.dispatch(EventsIntent.OnEventClicked(event))
                    onClickEvent(event)
                },
                onRefresh = { eventsViewModel.dispatch(EventsIntent.OnEventsRefreshed) },
                onClickRetry = { eventsViewModel.dispatch(EventsIntent.OnEventsRetried) },
                onPersonalizationRetry = { eventsViewModel.dispatch(EventsIntent.OnPersonalizationRetried) },
                showFilters = false,
            )
        },
        operatingNoticesContent = {
            OperatingNoticeScreen(
                uiState = noticeUiState,
                initialNoticeId = route.noticeId?.takeUnless { noticeOpened },
                onInitialNoticeOpened = { noticeOpened = true },
                onRefresh = { noticeViewModel.dispatch(OperatingNoticeIntent.OnRefreshed) },
                onRetry = { noticeViewModel.dispatch(OperatingNoticeIntent.OnRetried) },
                onShopClick = onShopClick,
                onPersonalizationRetry = { noticeViewModel.dispatch(OperatingNoticeIntent.OnPersonalizationRetried) },
                onNoticeClick = { notice ->
                    noticeViewModel.dispatch(OperatingNoticeIntent.OnNoticeClicked(notice))
                },
                isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
                onSourceClick = ExternalUriOpener::open,
                showFilters = false,
            )
        },
    )
}
