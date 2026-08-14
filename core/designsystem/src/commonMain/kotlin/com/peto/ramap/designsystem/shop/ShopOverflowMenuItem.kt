package com.peto.ramap.designsystem.shop

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
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_share

@Composable
internal fun ShopOverflowMenuItem(
    text: String,
    icon: DrawableResource,
    isActive: Boolean,
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
        leadingIcon = {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter =
                    ColorFilter.tint(
                        when {
                            isActive -> InstagramColor.Pink
                            else -> GrayColor.C300
                        },
                    ),
            )
        },
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun ShopOverflowMenuItemPreview() {
    RamapTheme {
        ShopOverflowMenuItem(
            text = "공유하기",
            icon = Res.drawable.ic_share,
            isActive = false,
            onClick = {},
        )
    }
}
