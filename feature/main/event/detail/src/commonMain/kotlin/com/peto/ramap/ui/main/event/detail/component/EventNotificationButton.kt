package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_notification_disable
import ramap.shared.generated.resources.event_notification_enable
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_notification_filled

@Composable
internal fun EventNotificationButton(
    uiState: EventDetailUiState,
    onNotificationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.isNotificationVisible) return
    IconButton(
        enabled = uiState.canChangeNotification && !uiState.isNotificationLoading,
        onClick = { onNotificationChanged(!uiState.isNotificationEnabled) },
        modifier = modifier.padding(end = 3.dp),
    ) {
        Icon(
            painter =
                painterResource(
                    if (uiState.isNotificationEnabled) {
                        Res.drawable.ic_notification_filled
                    } else {
                        Res.drawable.ic_notification
                    },
                ),
            contentDescription =
                stringResource(
                    if (uiState.isNotificationEnabled) {
                        Res.string.event_notification_disable
                    } else {
                        Res.string.event_notification_enable
                    },
                ),
            tint =
                if (uiState.isNotificationEnabled && !uiState.isNotificationLoading) {
                    InstagramColor.Pink
                } else {
                    GrayColor.C300
                },
        )
    }
}

@Preview
@Composable
private fun EventNotificationButton_Enabled_Preview() {
    RamapTheme {
        EventNotificationButton(
            uiState =
                EventDetailUiState(
                    isNotificationVisible = true,
                    canChangeNotification = true,
                    isNotificationEnabled = true,
                ),
            onNotificationChanged = {},
        )
    }
}

@Preview
@Composable
private fun EventNotificationButton_Disabled_Preview() {
    RamapTheme {
        EventNotificationButton(
            uiState =
                EventDetailUiState(
                    isNotificationVisible = true,
                    canChangeNotification = true,
                    isNotificationEnabled = false,
                ),
            onNotificationChanged = {},
        )
    }
}
