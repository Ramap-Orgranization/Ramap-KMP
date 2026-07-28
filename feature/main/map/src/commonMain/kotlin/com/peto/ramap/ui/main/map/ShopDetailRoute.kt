package com.peto.ramap.ui.main.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.platform.ShareLauncher
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.map.component.ShopDetailSheet
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message
import ramap.shared.generated.resources.share_shop_chooser_title
import ramap.shared.generated.resources.share_shop_message

@Composable
fun ShopDetailRoute(
    shopId: String,
    viewModel: MapViewModel,
    onDismiss: () -> Unit,
    onShowOnMap: (String) -> Unit,
    onEventNavigate: (ShopEvent) -> Unit = {},
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showLoginGuideDialog by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(Res.string.share_shop_chooser_title)

    LaunchedEffect(shopId) {
        viewModel.dispatch(MapIntent.OnShopIdSelected(shopId))
    }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            MapSideEffect.ShowLoginGuide -> showLoginGuideDialog = true
            is MapSideEffect.ShowToast ->
                toastManager.show(
                    sideEffect.data.copy(
                        action = sideEffect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )

            is MapSideEffect.ShareShop -> {
                val link = shopShareLinkFactory.create(sideEffect.shopId)
                val message =
                    getString(
                        Res.string.share_shop_message,
                        sideEffect.shopName,
                        link,
                    )
                ShareLauncher.share(
                    text = message,
                    chooserTitle = shareChooserTitle,
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ShopDetailSheet(
            uiState = uiState,
            isBackEnabled = true,
            maxHeight = maxHeight,
            showRequestedLoadingInSheet = true,
            onDismiss = {
                viewModel.dispatch(MapIntent.OnShopDetailDismissed)
                onDismiss()
            },
            onRetry = { viewModel.dispatch(MapIntent.OnShopDetailRetry) },
            onBookmarkToggled = { viewModel.dispatch(MapIntent.OnBookmarkToggled(it)) },
            onShopNotificationToggled = { shop ->
                val canToggleWithoutPermission =
                    !uiState.isLoggedIn ||
                        shop.id in uiState.hiddenShopIds ||
                        shop.id in uiState.notificationShopIds
                if (canToggleWithoutPermission) {
                    viewModel.dispatch(MapIntent.OnShopNotificationToggled(shop))
                } else {
                    coroutineScope.launch {
                        if (requestNotificationPermission()) {
                            viewModel.dispatch(MapIntent.OnShopNotificationToggled(shop))
                        } else {
                            toastManager.show(
                                ToastData(
                                    message = Res.string.notification_permission_enable_message,
                                    type = ToastType.DEFAULT,
                                    action =
                                        ToastAction(
                                            label = Res.string.location_permission_settings_action,
                                            onClick = appSettingsOpener::open,
                                        ),
                                ),
                            )
                        }
                    }
                }
            },
            onHiddenToggled = { viewModel.dispatch(MapIntent.OnHiddenToggled(it)) },
            onShopShareClick = { viewModel.dispatch(MapIntent.OnShopShareClicked(it)) },
            onShopMapLinkClick = { shop, provider -> viewModel.dispatch(MapIntent.OnShopMapLinkClicked(shop, provider)) },
            onEventClick = onEventNavigate,
            onReportSubmit = { wrongFields, description ->
                viewModel.dispatch(MapIntent.OnShopReportSubmitted(wrongFields, description))
            },
            onShowOnMap = { selectedShopId ->
                viewModel.dispatch(MapIntent.OnShopDetailDismissed)
                onShowOnMap(selectedShopId)
            },
        )
    }

    LoginGuideDialog(
        visible = showLoginGuideDialog,
        onDismiss = { showLoginGuideDialog = false },
        onConfirm = {
            showLoginGuideDialog = false
            viewModel.dispatch(MapIntent.OnKakaoLoginClicked)
        },
    )
}
