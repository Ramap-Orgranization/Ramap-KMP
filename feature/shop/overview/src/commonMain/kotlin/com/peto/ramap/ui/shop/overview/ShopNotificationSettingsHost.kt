package com.peto.ramap.ui.shop.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShopNotificationSettingsHost(
    originSource: AnalyticsSource,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
    viewModel: OverviewViewModel = koinViewModel(),
    content: @Composable ((RamenShop) -> Unit) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    DisposableEffect(viewModel) {
        onDispose { viewModel.dispatch(ShopOverviewIntent.OnOverviewDismissed) }
    }

    OverviewInteractions(
        uiState = uiState,
        viewModel = viewModel,
        originSource = originSource,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        requestNotificationPermission = requestNotificationPermission,
        isNotificationPermissionGranted = isNotificationPermissionGranted,
    ) { onShopNotificationToggled, _ ->
        content(onShopNotificationToggled)
    }
}
