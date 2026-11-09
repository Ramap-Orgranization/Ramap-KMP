package com.peto.ramap.ui.shop.component

import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ShareLauncher
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.login.rememberLoginGuideRequest
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.share_shop_chooser_title
import ramap.shared.generated.resources.share_shop_message

@Composable
fun MapInteractionHost(
    sideEffect: Flow<ShopInteractionEffect>,
    toastManager: ToastManager,
    appSettingsOpener: AppSettingsOpener,
    shopShareLinkFactory: ShopShareLinkFactory,
    onNotificationToggled: (RamenShop) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onLoginDismissed: () -> Unit,
    content: @Composable ((RamenShop) -> Unit, (StringResource) -> Unit) -> Unit,
) {
    val requestLoginGuide =
        rememberLoginGuideRequest(
            onLoginTypeSelected = onLoginTypeSelected,
            onDismiss = onLoginDismissed,
        )
    val shareChooserTitle = stringResource(Res.string.share_shop_chooser_title)
    ObserveAsEvents(sideEffect) { effect ->
        when (effect) {
            is ShopInteractionEffect.ShowLoginGuide -> {
                requestLoginGuide(effect.description)
            }
            is ShopInteractionEffect.ShowToast ->
                toastManager.show(
                    effect.data.copy(
                        action = effect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
            is ShopInteractionEffect.ShareShop -> {
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

    content(onNotificationToggled, requestLoginGuide)
}
