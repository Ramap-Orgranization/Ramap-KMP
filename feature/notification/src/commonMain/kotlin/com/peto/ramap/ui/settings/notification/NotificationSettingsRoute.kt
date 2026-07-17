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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LaduckLoadingContent
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSearchResultItem
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.component.eventDateText
import com.peto.ramap.ui.extension.stringResource
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventNotificationsEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnNotificationSettingsRetried
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsUiState
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget
import com.peto.ramap.ui.settings.notification.model.NotificationSettingsPermissionUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.event_notification_master_action
import ramap.shared.generated.resources.event_notification_settings_section
import ramap.shared.generated.resources.event_notification_subscribed_shops
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_permission_enable_message
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
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
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
    val coroutineScope = rememberCoroutineScope()
    var permissionUiState by remember { mutableStateOf(NotificationSettingsPermissionUiState()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            val isGranted = isNotificationPermissionGranted()
            permissionUiState = permissionUiState.onResume(isGranted)
            if (permissionUiState.shouldEnableServerNotifications) {
                permissionUiState = permissionUiState.consumePendingEnable()
                viewModel.dispatch(OnEventNotificationsEnabledChanged(true))
            }
        }
    }

    NotificationSettingsScreen(
        uiState = uiState.copy(areEnabled = permissionUiState.isEnabled(uiState.areEnabled)),
        onBack = onBack,
        onEventNotificationsEnabledChanged = { enabled ->
            if (enabled) {
                coroutineScope.launch {
                    if (requestNotificationPermission()) {
                        permissionUiState = permissionUiState.onEnableRequestResult(true)
                        viewModel.dispatch(OnEventNotificationsEnabledChanged(true))
                    } else {
                        permissionUiState = permissionUiState.onEnableRequestResult(false)
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
            } else {
                permissionUiState = permissionUiState.onDisabled()
                viewModel.dispatch(OnEventNotificationsEnabledChanged(false))
            }
        },
        onRemovalRequested = { viewModel.dispatch(OnRemovalRequested(it)) },
        onRemovalConfirmed = { viewModel.dispatch(OnRemovalConfirmed) },
        onRemovalDismissed = { viewModel.dispatch(OnRemovalDismissed) },
        onRetryClick = { viewModel.dispatch(OnNotificationSettingsRetried) },
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
    onRetryClick: () -> Unit,
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

        when (uiState.loadState) {
            LoadState.Idle, LoadState.Loading -> LaduckLoadingContent()
            LoadState.Error ->
                LoadErrorContent(
                    image = Res.drawable.laduck_error_confused,
                    title = stringResource(Res.string.settings_notification_menu),
                    description = stringResource(Res.string.data_load_failure_message),
                    onRetry = onRetryClick,
                )
            is LoadState.Content ->
                NotificationSettingsContent(
                    uiState = uiState,
                    onEventNotificationsEnabledChanged = onEventNotificationsEnabledChanged,
                    onRemovalRequested = onRemovalRequested,
                )
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

@Composable
private fun NotificationSettingsContent(
    uiState: NotificationSettingsUiState,
    onEventNotificationsEnabledChanged: (Boolean) -> Unit,
    onRemovalRequested: (NotificationRemovalTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            uiState.shops.values.forEach { shop ->
                RamenShopSearchResultItem(
                    shop = shop,
                    categoryLabel = { category -> stringResource(category.stringResource) },
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
                    dateText = eventDateText(event.startDate, event.endDate),
                    onClick = { onRemovalRequested(NotificationRemovalTarget.EventOverride(event.id)) },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
