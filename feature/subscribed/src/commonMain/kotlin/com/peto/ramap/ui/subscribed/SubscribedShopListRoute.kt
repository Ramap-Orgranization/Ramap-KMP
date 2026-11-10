package com.peto.ramap.ui.subscribed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.preview.ShopEventPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.subscribed.component.SubscribedRemovalConfirmDialog
import com.peto.ramap.ui.subscribed.component.SubscribedShopListContent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsUiState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.settings_subscribed_shops_menu

@Composable
fun SubscribedShopsRoute(
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onShopNotificationSettingsOpen: (RamenShop) -> Unit,
    onEventOpen: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: SubscribedShopsViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is SubscribedShopsSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    SubscribedShopsRouteContent(
        uiState = uiState,
        onBack = onBack,
        onShopOpen = onShopOpen,
        onShopNotificationSettingsOpen = onShopNotificationSettingsOpen,
        onEventOpen = onEventOpen,
        onRetry = { viewModel.dispatch(SubscribedShopsIntent.OnRetry) },
        onEventRemovalConfirmed = { viewModel.dispatch(SubscribedShopsIntent.OnEventRemovalConfirmed(it)) },
    )
}

@Composable
private fun SubscribedShopsRouteContent(
    uiState: SubscribedShopsUiState,
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onShopNotificationSettingsOpen: (RamenShop) -> Unit,
    onEventOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onEventRemovalConfirmed: (String) -> Unit,
) {
    var eventIdToRemove by remember { mutableStateOf<String?>(null) }

    SettingsListPage(
        title = Res.string.settings_subscribed_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        SubscribedShopListContent(
            uiState = uiState,
            onShopOpen = { onShopOpen(it.id) },
            onShopNotificationSettingsOpen = onShopNotificationSettingsOpen,
            onEventOpen = { onEventOpen(it.id) },
            onEventRemovalRequested = { eventIdToRemove = it },
        )
    }
    SubscribedRemovalConfirmDialog(
        visible = eventIdToRemove != null,
        onConfirm = {
            eventIdToRemove?.let(onEventRemovalConfirmed)
            eventIdToRemove = null
        },
        onDismiss = { eventIdToRemove = null },
    )
}

@Preview
@Composable
private fun SubscribedShopsRoutePreview() {
    RamapTheme {
        SubscribedShopsRouteContent(
            uiState =
                SubscribedShopsUiState(
                    shops = RamenShops(RamenShopPreviewParameterProvider().ramenShopPreviewSamples),
                    subscribedEvents = ShopEventPreviewParameterProvider().values.toList(),
                    haveShopsLoaded = true,
                    haveEventsLoaded = true,
                ),
            onBack = {},
            onShopOpen = {},
            onShopNotificationSettingsOpen = {},
            onEventOpen = {},
            onRetry = {},
            onEventRemovalConfirmed = {},
        )
    }
}
