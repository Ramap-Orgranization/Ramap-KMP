package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_report_warning_circle
import ramap.shared.generated.resources.shop_notification_permission_guide

@Composable
internal fun NotificationPermissionGuide(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .noRippleClickable(onClick = onOpenSettings),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_report_warning_circle),
                contentDescription = null,
                tint = SystemColor.Warning,
                modifier = Modifier.size(18.dp),
            )
            AppText(
                text = stringResource(Res.string.shop_notification_permission_guide),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionGuidePreview() {
    RamapTheme {
        NotificationPermissionGuide(
            onOpenSettings = {},
        )
    }
}
