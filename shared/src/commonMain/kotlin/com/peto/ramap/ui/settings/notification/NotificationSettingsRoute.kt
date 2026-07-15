package com.peto.ramap.ui.settings.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.core.base.ObserveAsEvents
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.component.RamenShopSearchResultItem
import com.peto.ramap.ui.settings.notification.contract.NotificationRemovalTarget
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect.ShowToast
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_notification_master_action
import ramap.shared.generated.resources.event_notification_settings_section
import ramap.shared.generated.resources.event_notification_subscribed_shops
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_removal_confirm_action
import ramap.shared.generated.resources.notification_removal_confirm_title
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_notification_menu
import ramap.shared.generated.resources.top_level_tab_event

@Composable
fun NotificationSettingsRoute(
    onBack: () -> Unit,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
) {
    val routeViewModelStore = remember { ViewModelStore() }
    val viewModelStoreOwner =
        remember(routeViewModelStore) {
            object : ViewModelStoreOwner {
                override val viewModelStore = routeViewModelStore
            }
        }
    val viewModel =
        koinViewModel<NotificationSettingsViewModel>(
            viewModelStoreOwner = viewModelStoreOwner,
        )

    DisposableEffect(routeViewModelStore) {
        onDispose(routeViewModelStore::clear)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is ShowToast ->
                toastManager.show(
                    sideEffect.data.copy(
                        action = sideEffect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
        }
    }

    NotificationSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onEventNotificationsEnabledChanged = { viewModel.dispatch(OnEnabledChanged(it)) },
        onRemovalRequested = { viewModel.dispatch(OnRemovalRequested(it)) },
        onRemovalConfirmed = { viewModel.dispatch(OnRemovalConfirmed) },
        onRemovalDismissed = { viewModel.dispatch(OnRemovalDismissed) },
    )
}

@Composable
fun NotificationSettingsScreen(
    uiState: NotificationSettingsUiState,
    onBack: () -> Unit,
    onEventNotificationsEnabledChanged: (Boolean) -> Unit,
    onRemovalRequested: (NotificationRemovalTarget) -> Unit,
    onRemovalConfirmed: () -> Unit,
    onRemovalDismissed: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_notification_menu),
            left = {
                Image(
                    painter = painterResource(Res.drawable.ic_arrow3_left),
                    contentDescription = stringResource(Res.string.navigation_back),
                    modifier =
                        Modifier
                            .padding(18.dp)
                            .size(24.dp)
                            .noRippleClickable(onClick = onBack),
                )
            },
        )

        SectionCard(
            title = stringResource(Res.string.event_notification_settings_section),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.event_notification_master_action),
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                )
                Switch(
                    checked = uiState.areEnabled,
                    onCheckedChange = onEventNotificationsEnabledChanged,
                )
            }
        }

        SectionCard(
            title = stringResource(Res.string.event_notification_subscribed_shops),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            uiState.shops.forEach { shop ->
                RamenShopSearchResultItem(
                    shop = shop,
                    onClick = { onRemovalRequested(NotificationRemovalTarget.Shop(shop.id)) },
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .border(1.dp, GrayColor.C200, RoundedCornerShape(16.dp)),
                    leadingContent = {
                        shop.instagramProfileImageUrl?.let { imageUrl ->
                            RemoteShopImage(
                                url = imageUrl,
                                modifier =
                                    Modifier
                                        .border(1.dp, GrayColor.C100, CircleShape)
                                        .size(40.dp)
                                        .clip(CircleShape),
                            )
                        }
                    },
                )
            }
        }

        SectionCard(
            title = stringResource(Res.string.top_level_tab_event),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            uiState.subscribedEvents.forEach { event ->
                EventCard(
                    event = event,
                    onClick = { onRemovalRequested(NotificationRemovalTarget.EventOverride(event.id)) },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }

    CommonDialog(
        visible = uiState.pendingRemoval != null,
        confirmText = stringResource(Res.string.notification_removal_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = onRemovalDismissed,
        content = {
            AppText(
                text = stringResource(Res.string.notification_removal_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = onRemovalConfirmed,
        onDismiss = onRemovalDismissed,
    )
}
