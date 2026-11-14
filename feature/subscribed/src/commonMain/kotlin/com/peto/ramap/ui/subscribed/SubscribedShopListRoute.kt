package com.peto.ramap.ui.subscribed

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.subscribed.component.SubscribedShopListContent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsUiState
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.notification_load_error_illustration
import ramap.shared.generated.resources.settings_subscribed_shops_menu

@Composable
fun SubscribedShopsRoute(
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onShopNotificationSettingsOpen: (RamenShop) -> Unit,
    viewModel: SubscribedShopsViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    SubscribedShopsRouteContent(
        uiState = uiState,
        onBack = onBack,
        onShopOpen = onShopOpen,
        onShopNotificationSettingsOpen = onShopNotificationSettingsOpen,
        onRetry = { viewModel.dispatch(SubscribedShopsIntent.OnRetry) },
    )
}

@Composable
private fun SubscribedShopsRouteContent(
    uiState: SubscribedShopsUiState,
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onShopNotificationSettingsOpen: (RamenShop) -> Unit,
    onRetry: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.settings_subscribed_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.notification_load_error_illustration,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        SubscribedShopListContent(
            shops = uiState.shops,
            onShopOpen = { onShopOpen(it.id) },
            onShopNotificationSettingsOpen = onShopNotificationSettingsOpen,
        )
    }
}

@Preview
@Composable
private fun SubscribedShopsRoutePreview() {
    RamapTheme {
        SubscribedShopsRouteContent(
            uiState =
                SubscribedShopsUiState(
                    shops = RamenShops(RamenShopPreviewParameterProvider().ramenShopPreviewSamples),
                    haveShopsLoaded = true,
                ),
            onBack = {},
            onShopOpen = {},
            onShopNotificationSettingsOpen = {},
            onRetry = {},
        )
    }
}
