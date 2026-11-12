package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.notification_load_error_illustration
import ramap.shared.generated.resources.retry_action
import ramap.shared.generated.resources.shop_notification_settings_load_failure
import ramap.shared.generated.resources.shop_notification_settings_refresh
import ramap.shared.generated.resources.shop_notification_settings_revision_message

@Composable
internal fun NotificationSettingsStatus(
    isLoading: Boolean,
    hasLoadFailure: Boolean,
    hasRevisionMismatch: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasLoadFailure && !hasRevisionMismatch && !isLoading) {
        LoadErrorContent(
            image = Res.drawable.notification_load_error_illustration,
            description = stringResource(Res.string.shop_notification_settings_load_failure),
            modifier =
                modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            onRetry = onRetry,
            compact = true,
        )
        return
    }

    val message =
        when {
            hasRevisionMismatch -> Res.string.shop_notification_settings_revision_message
            hasLoadFailure -> Res.string.shop_notification_settings_load_failure
            else -> null
        }
    if (message != null) {
        AppText(
            text = stringResource(message),
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(BrandColor.SubtleFill, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (hasLoadFailure || hasRevisionMismatch) {
        AppButton(
            text = stringResource(if (hasRevisionMismatch) Res.string.shop_notification_settings_refresh else Res.string.retry_action),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsStatusLoadFailurePreview() {
    RamapTheme {
        NotificationSettingsStatus(
            isLoading = false,
            hasLoadFailure = true,
            hasRevisionMismatch = false,
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsStatusRevisionMismatchPreview() {
    RamapTheme {
        NotificationSettingsStatus(
            isLoading = false,
            hasLoadFailure = false,
            hasRevisionMismatch = true,
            onRetry = {},
        )
    }
}
