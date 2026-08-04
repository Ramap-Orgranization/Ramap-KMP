package com.peto.ramap.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
fun CategoryFilterChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.C2,
    shape: Shape = RoundedCornerShape(999.dp),
) {
    val content = @Composable {
        AppText(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = style,
            color = if (selected) CommonColor.White else GrayColor.C400,
        )
    }
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = if (selected) GrayColor.C500 else CommonColor.White,
            border = BorderStroke(width = 1.dp, color = GrayColor.C200),
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = if (selected) GrayColor.C500 else CommonColor.White,
            border = BorderStroke(width = 1.dp, color = GrayColor.C200),
            onClick = onClick,
            content = content,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryFilterChipPreview() {
    RamapTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryFilterChip(
                label = "돈코츠",
                selected = true,
                onClick = {},
            )
            CategoryFilterChip(
                label = "쇼유",
                selected = false,
                onClick = {},
            )
        }
    }
}
