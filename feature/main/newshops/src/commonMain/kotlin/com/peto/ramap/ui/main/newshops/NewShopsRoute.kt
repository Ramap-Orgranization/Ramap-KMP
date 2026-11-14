package com.peto.ramap.ui.main.newshops

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.newshops.contract.NewShopsIntent
import com.peto.ramap.ui.main.newshops.contract.NewShopsSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

@Composable
fun NewShopsRoute(
    tabs: @Composable () -> Unit,
    onShopClick: (String) -> Unit,
) {
    val viewModel = koinViewModel<NewShopsViewModel>()
    val toastManager: ToastManager = koinInject()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentMonth by remember(viewModel) { mutableStateOf(viewModel.currentOpeningMonth()) }

    fun requestForCurrentMonth(intent: (ShopOpeningMonth) -> NewShopsIntent) {
        val month = viewModel.currentOpeningMonth()
        currentMonth = month
        viewModel.dispatch(intent(month))
    }

    val onShown = { requestForCurrentMonth { month -> NewShopsIntent.OnShown(month) } }
    val onRefresh = { requestForCurrentMonth { month -> NewShopsIntent.OnRefreshRequested(month) } }
    LaunchedEffect(viewModel) { onShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME, onEvent = onShown)
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            NewShopsSideEffect.ShowRefreshFailure ->
                toastManager.show(ToastData(Res.string.event_list_refresh_failure_message, ToastType.ERROR))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        tabs()
        NewShopsContent(
            uiState = uiState.visibleIn(currentMonth),
            onRefresh = onRefresh,
            onRetry = onRefresh,
            onShopClick = onShopClick,
            modifier = Modifier.weight(1f),
        )
    }
}
