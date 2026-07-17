package com.peto.ramap.ui.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LaduckLoadingContent
import com.peto.ramap.designsystem.component.LoadErrorContent
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
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.notification.model.NotificationSettingsPermissionUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.event_notification_master_action
import ramap.shared.generated.resources.event_notification_settings_section
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_permission_enable_message
import ramap.shared.generated.resources.settings_notification_menu

@Composable
fun NotificationSettingsRoute(
    onBack: () -> Unit,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var permissionState by remember { mutableStateOf(NotificationSettingsPermissionUiState()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            permissionState = permissionState.onResume(isNotificationPermissionGranted())
            if (permissionState.shouldEnableServerNotifications) {
                permissionState = permissionState.consumePendingEnable()
                viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(true))
            }
        }
    }
    NotificationSettingsScreen(
        loadState = uiState.loadState,
        enabled = permissionState.isEnabled(uiState.areEnabled),
        onBack = onBack,
        onRetry = { viewModel.dispatch(NotificationSettingsIntent.OnNotificationSettingsRetried) },
        onEnabledChanged = { enabled ->
            if (!enabled) {
                permissionState = permissionState.onDisabled()
                viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(false))
            } else {
                scope.launch {
                    if (requestNotificationPermission()) {
                        permissionState = permissionState.onEnableRequestResult(true)
                        viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(true))
                    } else {
                        permissionState = permissionState.onEnableRequestResult(false)
                        toastManager.show(
                            ToastData(
                                Res.string.notification_permission_enable_message,
                                ToastType.DEFAULT,
                                action = ToastAction(Res.string.location_permission_settings_action, appSettingsOpener::open),
                            ),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun NotificationSettingsScreen(
    loadState: LoadState<Unit>,
    enabled: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_notification_menu),
            left = {
                Image(
                    painterResource(Res.drawable.ic_arrow3_left),
                    stringResource(Res.string.navigation_back),
                    Modifier.padding(18.dp).size(24.dp).noRippleClickable(onClick = onBack),
                )
            },
        )
        when (loadState) {
            LoadState.Idle, LoadState.Loading -> LaduckLoadingContent()
            LoadState.Error ->
                LoadErrorContent(
                    Res.drawable.laduck_error_confused,
                    stringResource(Res.string.settings_notification_menu),
                    stringResource(Res.string.data_load_failure_message),
                    onRetry = onRetry,
                )
            is LoadState.Content ->
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
                            stringResource(Res.string.event_notification_master_action),
                            AppTextStyle.B1,
                            GrayColor.C500,
                        )
                        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
                    }
                }
        }
    }
}
