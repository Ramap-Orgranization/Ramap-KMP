package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.RecommendationColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.recommendation_brand
import ramap.shared.generated.resources.recommendation_title
import ramap.shared.generated.resources.woongseong_recommendation_trademark

@Composable
internal fun RecommendationListHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { heading() },
        ) {
            AppText(
                text = stringResource(Res.string.recommendation_brand),
                style = AppTextStyle.H3,
                color = GrayColor.C500,
            )
            AppText(
                text = stringResource(Res.string.recommendation_title),
                style = AppTextStyle.H3,
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .padding(start = 5.dp)
                        .drawBehind {
                            val underlineHeight = 7.dp.toPx()
                            drawRect(
                                color = BrandColor.Fill.copy(alpha = 0.5f),
                                topLeft = Offset(x = 0f, y = size.height - underlineHeight - 2.dp.toPx()),
                                size = Size(width = size.width, height = underlineHeight),
                            )
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .background(RecommendationColor.Cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.woongseong_recommendation_trademark),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(width = 1.dp, color = RecommendationColor.Brown, shape = CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun RecommendationListHeaderPreview() {
    RamapTheme {
        RecommendationListHeader()
    }
}
