package com.peto.ramap.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.RecommendationColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CategoryFilterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    style: AppTextStyle = AppTextStyle.C2,
    trailingIcon: DrawableResource? = null,
    leadingIcon: DrawableResource? = null,
) {
    Surface(
        modifier =
            modifier.semantics {
                this.selected = selected
                role = if (trailingIcon == null) Role.Tab else Role.Button
            },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) BrandColor.SubtleFill else CommonColor.White,
        border = if (selected) BorderStroke(1.dp, RecommendationColor.Brown) else null,
        onClick = onClick,
        shadowElevation = 6.dp,
        content = {
            val contentColor = if (selected) BrandColor.SecondPrimary else GrayColor.C400
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                AppText(text = label, style = style, color = contentColor)
                trailingIcon?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
    )
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
