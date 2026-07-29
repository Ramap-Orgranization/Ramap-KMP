package com.peto.ramap.ui.main.ranking.component

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down

@Composable
internal fun AreaFilterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = CommonColor.White,
        border = BorderStroke(1.dp, GrayColor.C100),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppText(text = label, style = AppTextStyle.B4, color = GrayColor.C500)
            Icon(
                painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = CommonColor.Black,
            )
        }
    }
}

@Composable
@Preview
private fun AreaFilterChipPreview() {
    RamapTheme {
        AreaFilterChip(label = "전국", onClick = {})
    }
}
