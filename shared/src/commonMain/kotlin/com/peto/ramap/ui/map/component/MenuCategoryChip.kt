package com.peto.ramap.ui.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun MenuCategoryChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = GrayColor.C100,
        border = BorderStroke(1.dp, GrayColor.C200),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
    ) {
        AppText(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = AppTextStyle.C2,
            color = GrayColor.C500,
        )
    }
}
