package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_notification_settings_cancel
import ramap.shared.generated.resources.shop_notification_settings_save

@Composable
internal fun NotificationSettingsActions(
    canSave: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = GrayColor.C100)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton(
                text = stringResource(Res.string.shop_notification_settings_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                backgroundColor = GrayColor.C050,
                textColor = GrayColor.C400,
                cornerRadius = 16.dp,
            )
            AppButton(
                text = stringResource(Res.string.shop_notification_settings_save),
                onClick = onSave,
                modifier = Modifier.weight(2f),
                enabled = canSave,
                isLoading = isSaving,
                disabledBackgroundColor = if (isSaving) GrayColor.C500 else GrayColor.C100,
                disabledTextColor = if (isSaving) CommonColor.White else GrayColor.C300,
                cornerRadius = 16.dp,
            )
        }
    }
}
