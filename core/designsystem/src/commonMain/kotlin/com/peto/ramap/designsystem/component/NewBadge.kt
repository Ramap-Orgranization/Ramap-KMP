package com.peto.ramap.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.announcement_new
import ramap.shared.generated.resources.new_badge

@Composable
fun NewBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ChromaticColor.Orange400,
) {
    val description = stringResource(Res.string.announcement_new)
    Box(
        modifier =
            modifier
                .size(18.dp)
                .background(backgroundColor, CircleShape)
                .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = stringResource(Res.string.new_badge),
            style = AppTextStyle.C3,
            color = CommonColor.White,
        )
    }
}
