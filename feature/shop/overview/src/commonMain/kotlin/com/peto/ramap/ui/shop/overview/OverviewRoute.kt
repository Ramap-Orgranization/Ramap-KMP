package com.peto.ramap.ui.shop.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.shop.component.MapInteractionHost
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.overview.component.NotificationSettingsDialog
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.overview.model.OverviewRouteActions
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_notification_settings_save_failure

@Composable
fun OverviewRoute(
    shopId: String,
    onDismissRequest: () -> Unit,
    onShopViewed: (RamenShop) -> Unit,
    onShopLoadFailed: (String) -> Unit,
    onLoginGuideRequested: () -> Unit,
    initialShop: RamenShop? = null,
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
    viewModel: OverviewViewModel = koinViewModel(),
    content: @Composable (ShopOverviewState, OverviewRouteActions) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionScope = rememberCoroutineScope()
    val notificationToastManager = remember { ToastManager() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRequestNotificationPermission by rememberUpdatedState(requestNotificationPermission)
    val currentIsNotificationPermissionGranted by rememberUpdatedState(isNotificationPermissionGranted)
    val currentOnLoginGuideRequested by rememberUpdatedState(onLoginGuideRequested)
    val currentOnShopViewed by rememberUpdatedState(onShopViewed)
    val currentOnShopLoadFailed by rememberUpdatedState(onShopLoadFailed)
    val interactions =
        remember(viewModel) {
            viewModel.sideEffect
                .mapNotNull { effect ->
                    when (effect) {
                        ShopOverviewSideEffect.ShowNotificationSaveFailure -> {
                            notificationToastManager.show(
                                ToastData(message = Res.string.shop_notification_settings_save_failure, type = ToastType.ERROR),
                            )
                            null
                        }
                        is ShopOverviewSideEffect.RequestNotificationPermission -> {
                            permissionScope.launch {
                                if (viewModel.uiState.value.notificationPermissionGeneration != effect.generation) return@launch
                                var granted = false
                                try {
                                    granted = currentRequestNotificationPermission()
                                } finally {
                                    viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(effect.generation, granted))
                                }
                            }
                            null
                        }
                        is ShopOverviewSideEffect.Interaction -> {
                            if (effect.effect is ShopInteractionEffect.ShowLoginGuide) currentOnLoginGuideRequested()
                            effect.effect
                        }
                        is ShopOverviewSideEffect.ShopViewed -> {
                            currentOnShopViewed(effect.shop)
                            null
                        }
                    }
                }.buffer(0)
        }

    DisposableEffect(viewModel, shopId) {
        onDispose { viewModel.dispatch(ShopOverviewIntent.OnOverviewDismissed) }
    }

    LaunchedEffect(viewModel, shopId) {
        if (initialShop == null) {
            viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected(shopId))
        } else {
            viewModel.dispatch(ShopOverviewIntent.OnShopSelected(initialShop))
        }
    }

    LaunchedEffect(uiState.hasLoadFailure) {
        if (uiState.hasLoadFailure && uiState.shopOverview == null) currentOnShopLoadFailed(shopId)
    }

    LaunchedEffect(viewModel, lifecycleOwner, uiState.notificationSettingsShopId, uiState.notificationPermissionGeneration) {
        if (uiState.notificationSettingsShopId == null || uiState.notificationPermissionGeneration != null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionChecked(currentIsNotificationPermissionGranted()))
        }
    }

    MapInteractionHost(
        sideEffect = interactions,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        onNotificationToggled = {
            viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(it, originSource))
        },
        onLoginTypeSelected = { viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(it)) },
        onLoginDismissed = { viewModel.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed) },
    ) { onShopNotificationToggled, requestLoginGuide ->
        content(
            uiState,
            OverviewRouteActions(
                onDismissRequest = {
                    viewModel.dispatch(ShopOverviewIntent.OnOverviewDismissed)
                    onDismissRequest()
                },
                onRetry = { viewModel.dispatch(ShopOverviewIntent.OnOverviewRetry) },
                onLoginGuideRequested = requestLoginGuide,
                onBookmarkToggled = { viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(it, originSource)) },
                onShopNotificationToggled = onShopNotificationToggled,
                onShopShareClick = { viewModel.dispatch(ShopOverviewIntent.OnShopShareClicked(it)) },
                onShopMapLinkClick = { shop, provider -> viewModel.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, provider)) },
                onReportSubmit = { wrongFields, description -> viewModel.dispatch(ShopOverviewIntent.OnShopReportSubmitted(wrongFields, description)) },
                onAppleMapsClick = { shop ->
                    viewModel.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, "apple"))
                    ExternalUriOpener.openAppleMaps(
                        name = shop.name,
                        address = shop.address,
                        latitude = shop.location.lat,
                        longitude = shop.location.lng,
                    )
                },
            ),
        )
    }

    if (uiState.notificationSettingsShopId != null) {
        NotificationSettingsDialog(
            catalog = uiState.notificationSettings?.catalog,
            selection = uiState.notificationDraft,
            isLoading = uiState.isNotificationSettingsLoading,
            isSaving = uiState.isNotificationSettingsSaving,
            hasLoadFailure = uiState.hasNotificationLoadFailure,
            hasRevisionMismatch = uiState.hasNotificationRevisionMismatch,
            showPermissionGuide = uiState.isNotificationPermissionGranted == false,
            onTypeToggled = { viewModel.dispatch(ShopOverviewIntent.OnNotificationTypeToggled(it)) },
            onSelectAll = { viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll) },
            onClear = { viewModel.dispatch(ShopOverviewIntent.OnNotificationClear) },
            onDismiss = { viewModel.dispatch(ShopOverviewIntent.OnNotificationCancel) },
            onRetry = { viewModel.dispatch(ShopOverviewIntent.OnNotificationRetry) },
            onSave = { viewModel.dispatch(ShopOverviewIntent.OnNotificationSave) },
            onOpenSettings = appSettingsOpener::open,
            toastHost = { modifier ->
                ToastHost(toastManager = notificationToastManager, modifier = modifier)
            },
        )
    }
}
