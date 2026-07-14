package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.ui.event.EventDetailRoute
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.my.MyTabRoute
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_exit_back_message
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun AppRoute(
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
) {
    val navigationState = rememberNavigationState()
    val mapViewModel: MapViewModel = koinViewModel()
    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    var previousBackPress by remember { mutableStateOf(TimeSource.Monotonic.markNow() - EXIT_INTERVAL) }

    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = navigationState.backStack.size > 1 || onExitRequested != null,
        onBackCompleted = {
            if (navigationState.backStack.size > 1) {
                navigationState.pop()
                return@NavigationBackHandler
            }

            val now = TimeSource.Monotonic.markNow()
            if (now - previousBackPress <= EXIT_INTERVAL) {
                onExitRequested?.invoke()
            } else {
                previousBackPress = now
                toastManager.tryShow(
                    ToastData(
                        message = Res.string.app_exit_back_message,
                        type = ToastType.DEFAULT,
                    ),
                )
            }
        },
    )

    NavigationRouter(
        currentRoute = navigationState.currentRoute,
        selectedTab = navigationState.selectedTab,
        onTabSelected = navigationState::selectTopLevelTab,
        mapContent = {
            MapRoute(
                isBackEnabled = navigationState.currentRoute !is ScreenRoutes.EventDetailRoutes,
                onEventNavigate = navigationState::showEvent,
                viewModel = mapViewModel,
            )
        },
        myContent = {
            MyTabRoute(onHiddenShopsNavigate = navigationState::showHiddenShops)
        },
        hiddenContent = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
                onShopClick = { shop ->
                    mapViewModel.dispatch(OnShopSelected(shop))
                    navigationState.showMap()
                },
            )
        },
        eventContent = {
            EventDetailRoute(
                event = requireNotNull(navigationState.selectedEvent),
                onBack = navigationState::pop,
                onShopClick = { shopId ->
                    mapViewModel.dispatch(OnShopIdSelected(shopId))
                    navigationState.showMap()
                },
            )
        },
    )
}

private val EXIT_INTERVAL = 2.seconds
