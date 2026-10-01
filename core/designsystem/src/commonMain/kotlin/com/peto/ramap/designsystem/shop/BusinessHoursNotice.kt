package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_info
import ramap.shared.generated.resources.shop_detail_business_hours_notice

@Composable
internal fun BusinessHoursNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = GrayColor.C300,
        )
        AppText(
            text = stringResource(Res.string.shop_detail_business_hours_notice),
            style = AppTextStyle.C1,
            color = GrayColor.C300,
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
