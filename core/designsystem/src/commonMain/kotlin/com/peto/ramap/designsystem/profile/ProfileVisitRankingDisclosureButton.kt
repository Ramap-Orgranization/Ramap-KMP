package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import ramap.shared.generated.resources.review_collapse
import ramap.shared.generated.resources.review_more

@Composable
internal fun ProfileVisitRankingDisclosureButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .noRippleClickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(if (expanded) Res.string.review_collapse else Res.string.review_more),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
        Icon(
            painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
            contentDescription = null,
            tint = GrayColor.C400,
            modifier =
                Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 180f else 0f),
        )
    }
}
