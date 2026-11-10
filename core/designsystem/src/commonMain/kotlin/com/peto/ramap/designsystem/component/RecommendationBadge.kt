package com.peto.ramap.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.RecommendationColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.recommendation_for
import ramap.shared.generated.resources.recommendation_shop_count
import ramap.shared.generated.resources.woongseong_recommendation_trademark

@Composable
fun RecommendationBadge(
    modifier: Modifier = Modifier,
    shopCount: Int? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            modifier
                .padding(bottom = 1.dp)
                .background(RecommendationColor.Cream, shape)
                .border(1.dp, RecommendationColor.Border, shape)
                .padding(start = 5.dp, end = 9.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.woongseong_recommendation_trademark),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        AppText(
            text = stringResource(Res.string.recommendation_for),
            style = AppTextStyle.B1,
            color = RecommendationColor.Brown,
        )
        if (shopCount != null) {
            Spacer(Modifier.weight(1f))
            AppText(
                text = stringResource(Res.string.recommendation_shop_count, shopCount),
                style = AppTextStyle.B2,
                color = RecommendationColor.Brown,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecommendationBadgePreview() {
    RamapTheme {
        RecommendationBadge()
    }
}

@Preview(showBackground = true)
@Composable
private fun RecommendationBadgeWithCountPreview() {
    RamapTheme {
        RecommendationBadge(shopCount = 3)
    }
}
