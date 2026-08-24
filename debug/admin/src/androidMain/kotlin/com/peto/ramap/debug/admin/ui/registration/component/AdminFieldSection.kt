package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun AdminFieldSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminFieldSectionPreview() {
    RamapTheme {
        AdminFieldSection(label = "라벨") {
            AppText(text = "콘텐츠", style = AppTextStyle.B2, color = GrayColor.C500)
        }
    }
}
