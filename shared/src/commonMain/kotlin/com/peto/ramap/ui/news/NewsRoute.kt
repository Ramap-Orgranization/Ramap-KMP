package com.peto.ramap.ui.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.NewsFilters
import com.peto.ramap.designsystem.tab.AppChipTabs
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.news_tab_event
import ramap.shared.generated.resources.news_tab_operating_notice

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

    @Composable
    fun HeaderContent(
        query: String,
        savedOnly: Boolean,
        onQueryChanged: (String) -> Unit,
        onSavedToggled: () -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            NewsFilters(
                query = query,
                savedOnly = savedOnly,
                onQueryChanged = onQueryChanged,
                onSavedToggled = onSavedToggled,
            )
            AppChipTabs(
                labels =
                    NewsTab.entries.map { tab ->
                        stringResource(
                            when (tab) {
                                NewsTab.EVENTS -> Res.string.news_tab_event
                                NewsTab.OPERATING_NOTICES -> Res.string.news_tab_operating_notice
                            },
                        )
                    },
                selectedIndex = activeTab.ordinal,
                onSelect = { index -> navigationState.selectNewsTab(NewsTab.entries[index]) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    NewsScreen(
        selectedTab = activeTab,
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
                headerContent = {
                    HeaderContent(
                        query = eventsUiState.query,
                        savedOnly = eventsUiState.savedOnly,
                        onQueryChanged = { query -> eventsViewModel.dispatch(EventsIntent.OnQueryChanged(query)) },
                        onSavedToggled = { eventsViewModel.dispatch(EventsIntent.OnSavedToggled) },
                    )
                },
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
                headerContent = {
                    HeaderContent(
                        query = noticeUiState.query,
                        savedOnly = noticeUiState.savedOnly,
                        onQueryChanged = { query -> noticeViewModel.dispatch(OperatingNoticeIntent.OnQueryChanged(query)) },
                        onSavedToggled = { noticeViewModel.dispatch(OperatingNoticeIntent.OnSavedToggled) },
                    )
                },
            )
        },
    )
}
