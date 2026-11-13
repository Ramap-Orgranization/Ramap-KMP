package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_share

@Composable
internal fun OverflowMenuItem(
    text: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        modifier = modifier,
        text = {
            AppText(
                text = text,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
        },
        leadingIcon = leadingIcon,
        onClick = onClick,
    )
}

@Composable
internal fun OverflowMenuIcon(
    icon: DrawableResource,
    isActive: Boolean = false,
) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        colorFilter = ColorFilter.tint(if (isActive) BrandColor.SecondPrimary else GrayColor.C300),
    )
}

@Preview(showBackground = true)
@Composable
private fun OverflowMenuItemPreview() {
    RamapTheme {
        OverflowMenuItem(
            text = "공유하기",
            leadingIcon = { OverflowMenuIcon(icon = Res.drawable.ic_share) },
            onClick = {},
        )
    }
}
