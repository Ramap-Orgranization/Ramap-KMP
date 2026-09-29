package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_profile_bookmark
import ramap.shared.generated.resources.settings_bookmarked_shops_menu

@Composable
internal fun MyMenuRow(
    icon: DrawableResource,
    title: StringResource,
    count: Int?,
    iconBackground: Color,
    iconTint: Color,
    hasCount: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
        }
        AppText(
            text = stringResource(title),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        )
        if (hasCount && isLoading && count == null) {
            Skeleton(
                modifier = Modifier.size(width = 24.dp, height = 18.dp),
                shape = RoundedCornerShape(8.dp),
            )
        } else if (count != null) {
            AppText(
                text = count.toString(),
                style = AppTextStyle.C2,
                color = iconTint,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBackground)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyMenuRowPreview() {
    RamapTheme {
        MyMenuRow(
            icon = Res.drawable.ic_profile_bookmark,
            title = Res.string.settings_bookmarked_shops_menu,
            count = 12,
            iconBackground = ChromaticColor.Orange050,
            iconTint = ChromaticColor.Orange300,
            onClick = {},
        )
    }
}
