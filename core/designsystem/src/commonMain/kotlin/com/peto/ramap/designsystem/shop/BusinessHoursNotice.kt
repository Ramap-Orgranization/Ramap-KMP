package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_notice

@Composable
internal fun BusinessHoursNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(14.dp)
                    .border(1.dp, GrayColor.C500, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = "!",
                style = AppTextStyle.B4,
                color = GrayColor.C500,
            )
        }
        AppText(
            text = stringResource(Res.string.shop_detail_business_hours_notice),
            style = AppTextStyle.B4,
            color = GrayColor.C500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BusinessHoursNoticePreview() {
    RamapTheme {
        BusinessHoursNotice()
    }
}
