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
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
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
        onEventOpen = onEventOpen,
        onRetry = { viewModel.dispatch(SubscribedShopsIntent.OnRetry) },
        onRemovalConfirmed = { viewModel.dispatch(SubscribedShopsIntent.OnRemovalConfirmed(it)) },
    )
}

@Composable
private fun SubscribedShopsRouteContent(
    uiState: SubscribedShopsUiState,
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onEventOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onRemovalConfirmed: (SubscribedRemovalTarget) -> Unit,
) {
    var removalTarget by remember { mutableStateOf<SubscribedRemovalTarget?>(null) }

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
            onEventOpen = { onEventOpen(it.id) },
            onRemovalRequested = { removalTarget = it },
        )
    }
    SubscribedRemovalConfirmDialog(
        visible = removalTarget != null,
        onConfirm = {
            removalTarget?.let { target ->
                onRemovalConfirmed(target)
            }
            removalTarget = null
        },
        onDismiss = { removalTarget = null },
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
            onEventOpen = {},
            onRetry = {},
            onRemovalConfirmed = {},
        )
    }
}
