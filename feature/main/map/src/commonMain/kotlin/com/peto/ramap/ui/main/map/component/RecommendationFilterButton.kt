package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RecommendationColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_app_logo_cutout
import ramap.shared.generated.resources.recommendation_title

@Composable
internal fun RecommendationFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.recommendation_title)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                AppText(text = title, style = AppTextStyle.B2, color = LocalContentColor.current)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = title
                        selected = isActive
                    }.shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                    ),
            shape = CircleShape,
            color = if (isActive) RecommendationColor.Gold else CommonColor.White,
            border = if (isActive) BorderStroke(2.dp, RecommendationColor.Brown) else null,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(Res.drawable.ic_app_logo_cutout),
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            width = 1.dp,
                            color = RecommendationColor.Brown,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}
