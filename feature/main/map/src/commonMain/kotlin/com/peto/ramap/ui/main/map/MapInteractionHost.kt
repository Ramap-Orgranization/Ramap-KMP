package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ShareLauncher
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message
import ramap.shared.generated.resources.share_shop_chooser_title
import ramap.shared.generated.resources.share_shop_message

@Composable
internal fun MapInteractionHost(
    sideEffect: Flow<MapSideEffect>,
    isLoggedIn: Boolean,
    hiddenShopIds: Set<String>,
    notificationShopIds: Set<String>,
    toastManager: ToastManager,
    appSettingsOpener: AppSettingsOpener,
    shopShareLinkFactory: ShopShareLinkFactory,
    requestNotificationPermission: suspend () -> Boolean,
    onNotificationToggled: (RamenShop) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onLoginDismissed: () -> Unit,
    content: @Composable ((RamenShop) -> Unit) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showLoginGuideDialog by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(Res.string.share_shop_chooser_title)
    ObserveAsEvents(sideEffect) { effect ->
        when (effect) {
            MapSideEffect.ShowLoginGuide -> {
                showLoginGuideDialog = true
            }
            is MapSideEffect.ShowToast ->
                toastManager.show(
                    effect.data.copy(
                        action = effect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
            is MapSideEffect.ShareShop -> {
                val link = shopShareLinkFactory.create(effect.shopId)
                val message =
                    getString(
                        Res.string.share_shop_message,
                        effect.shopName,
                        link,
                    )
                ShareLauncher.share(message, shareChooserTitle)
            }
        }
    }

    content { shop ->
        val canToggleWithoutPermission =
            !isLoggedIn ||
                shop.id in hiddenShopIds ||
                shop.id in notificationShopIds
        if (canToggleWithoutPermission) {
            onNotificationToggled(shop)
        } else {
            coroutineScope.launch {
                if (requestNotificationPermission()) {
                    onNotificationToggled(shop)
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
    }

    LoginGuideDialog(
        visible = showLoginGuideDialog,
        onDismiss = {
            showLoginGuideDialog = false
            onLoginDismissed()
        },
        onLoginTypeSelected = { type ->
            showLoginGuideDialog = false
            onLoginTypeSelected(type)
        },
        loginButton = { type, onClick -> LoginButton(type, onClick) },
    )
}
