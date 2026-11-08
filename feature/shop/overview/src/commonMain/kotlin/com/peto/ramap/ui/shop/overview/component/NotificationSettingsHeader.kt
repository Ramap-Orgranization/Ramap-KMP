package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.shop_notification_settings_subtitle
import ramap.shared.generated.resources.shop_notification_settings_title

@Composable
internal fun NotificationSettingsHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(GrayColor.C500, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_notification),
                contentDescription = null,
                tint = CommonColor.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppText(
                text = stringResource(Res.string.shop_notification_settings_title),
                style = AppTextStyle.H3,
                color = GrayColor.C500,
                modifier = Modifier.semantics { heading() },
            )
            AppText(
                text = stringResource(Res.string.shop_notification_settings_subtitle),
                style = AppTextStyle.C1,
                color = GrayColor.C300,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsHeaderPreview() {
    RamapTheme {
        NotificationSettingsHeader()
    }
}
