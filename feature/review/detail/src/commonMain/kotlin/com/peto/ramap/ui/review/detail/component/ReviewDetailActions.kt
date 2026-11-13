package com.peto.ramap.ui.review.detail.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor

@Composable
internal fun ReviewDetailTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppText(
        text = text,
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .noRippleClickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 10.dp),
        style = AppTextStyle.B2,
        color = if (enabled) BrandColor.SecondPrimary else GrayColor.C300,
    )
}
