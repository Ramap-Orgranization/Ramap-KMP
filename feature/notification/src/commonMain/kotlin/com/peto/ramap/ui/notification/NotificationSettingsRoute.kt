package com.peto.ramap.ui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.designsystem.topbar.BackTopBar
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.notification.contract.NotificationSettingsLoadKey
import com.peto.ramap.ui.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.notification.contract.NotificationSettingsUiState
import com.peto.ramap.ui.notification.model.NotificationSettingsPermissionUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_notification_master_action
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message
import ramap.shared.generated.resources.review_notification_master_action
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
    val eventPermission = remember { mutableStateOf(NotificationSettingsPermissionUiState()) }
    val reviewAccountId = uiState.reviewAccountId
    val reviewPermission = remember(reviewAccountId) { mutableStateOf(NotificationSettingsPermissionUiState()) }
    val updateEvent: (Boolean) -> Unit = {
        viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(it))
    }
    val updateReview: (Boolean) -> Unit = {
        if (reviewAccountId != null) {
            viewModel.dispatch(NotificationSettingsIntent.OnReviewNotificationsEnabledChanged(reviewAccountId, it))
        }
    }

    fun changeEnabled(
        enabled: Boolean,
        permission: MutableState<NotificationSettingsPermissionUiState>,
        update: (Boolean) -> Unit,
    ) {
        if (!enabled) {
            permission.value = permission.value.onDisabled()
            update(false)
            return
        }
        scope.launch {
            val granted = requestNotificationPermission()
            eventPermission.value = eventPermission.value.onResume(granted)
            reviewPermission.value = reviewPermission.value.onResume(granted)
            permission.value = permission.value.onEnableRequestResult(granted)
            if (granted) {
                update(true)
            } else {
                toastManager.show(
                    ToastData(
                        Res.string.notification_permission_enable_message,
                        ToastType.DEFAULT,
                        action =
                            ToastAction(
                                Res.string.location_permission_settings_action,
                                appSettingsOpener::open,
                            ),
                    ),
                )
            }
        }
    }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NotificationSettingsSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            val granted = isNotificationPermissionGranted()
            eventPermission.value = eventPermission.value.onResume(granted)
            reviewPermission.value = reviewPermission.value.onResume(granted)
            if (eventPermission.value.shouldEnableServerNotifications) {
                eventPermission.value = eventPermission.value.consumePendingEnable()
                updateEvent(true)
            }
            if (reviewPermission.value.shouldEnableServerNotifications) {
                reviewPermission.value = reviewPermission.value.consumePendingEnable()
                updateReview(true)
            }
        }
    }
    NotificationSettingsScreen(
        uiState = uiState,
        enabled = eventPermission.value.isEnabled(uiState.areEnabled),
        onBack = onBack,
        onRetry = { viewModel.dispatch(NotificationSettingsIntent.OnNotificationSettingsRetried) },
        onEnabledChanged = { changeEnabled(it, eventPermission, updateEvent) },
        onReviewRetry = { viewModel.dispatch(NotificationSettingsIntent.OnReviewNotificationSettingsRetried) },
        onReviewEnabledChanged = { changeEnabled(it, reviewPermission, updateReview) },
    )
}

@Composable
private fun NotificationSettingsScreen(
    uiState: NotificationSettingsUiState,
    enabled: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onReviewRetry: () -> Unit,
    onReviewEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackTopBar(
            onBack = onBack,
            title = stringResource(Res.string.settings_notification_menu),
        )
        SectionCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            NotificationSettingRow(
                label = stringResource(Res.string.event_notification_master_action),
                checked = enabled,
                switchEnabled = !uiState.loadState.isLoading(NotificationSettingsLoadKey.FETCH),
                showError = uiState.showError,
                onRetry = onRetry,
                onCheckedChange = onEnabledChanged,
            )
            NotificationSettingRow(
                label = stringResource(Res.string.review_notification_master_action),
                checked = uiState.reviewNotificationsEnabled == true,
                switchEnabled =
                    uiState.reviewNotificationsEnabled != null &&
                        !uiState.loadState.isLoading(NotificationSettingsLoadKey.FETCH_REVIEW) &&
                        !uiState.loadState.isLoading(NotificationSettingsLoadKey.UPDATE_REVIEW),
                showError = uiState.showReviewSettingsError,
                onRetry = onReviewRetry,
                onCheckedChange = onReviewEnabledChanged,
            )
        }
    }
}

@Composable
private fun NotificationSettingRow(
    label: String,
    checked: Boolean,
    switchEnabled: Boolean,
    showError: Boolean,
    onRetry: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(label, AppTextStyle.T2, GrayColor.C500)
        if (showError) {
            RetryButton(onClick = onRetry)
        } else {
            Switch(
                checked = checked,
                enabled = switchEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
