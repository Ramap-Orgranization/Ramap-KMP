package com.peto.ramap.designsystem.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.operating_notice_saved
import ramap.shared.generated.resources.ranking_all_regions

@Composable
fun AppFilterChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    icon: DrawableResource? = null,
    trailingIcon: DrawableResource? = null,
    selected: Boolean = false,
) {
    Surface(
        modifier = modifier.semantics { this.selected = selected }.noRippleClickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) GrayColor.C050 else CommonColor.White,
        border = BorderStroke(1.dp, if (selected) GrayColor.C500 else GrayColor.C100),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = GrayColor.C500,
                    modifier = Modifier.size(18.dp),
                )
            }
            AppText(
                text = label,
                style = AppTextStyle.B3,
                color = GrayColor.C500,
            )
            trailingIcon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = GrayColor.C400,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(name = "필터 칩 선택 상태", showBackground = true)
@Composable
private fun AppFilterChipPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppFilterChip(
                label = stringResource(Res.string.ranking_all_regions),
                onClick = {},
                trailingIcon = Res.drawable.ic_keyboard_arrow_down,
            )
            AppFilterChip(
                label = stringResource(Res.string.operating_notice_saved),
                onClick = {},
                icon = Res.drawable.ic_kid_star,
            )
            AppFilterChip(
                label = stringResource(Res.string.operating_notice_saved),
                onClick = {},
                icon = Res.drawable.ic_kid_star_filled,
                selected = true,
            )
        }
    }
}
