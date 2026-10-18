package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_shop_detail_error_title

@Composable
fun ShopReviewCardHeader(
    review: Review,
    shop: RamenShop?,
    onOpenShopDetail: (() -> Unit)?,
    actions: ReviewCardActions,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    shopName: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 5.dp, top = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shop != null) {
            RamenShopSummary(
                shop = shop,
                categoryLabel = { stringResource(CategoryResourceMapper.label(it)) },
                containerColor = CommonColor.White,
                leadingContent = {},
                onClick = onOpenShopDetail,
                modifier = Modifier.weight(1f),
            )
        } else {
            AppText(
                text = shopName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.map_shop_detail_error_title),
                style = AppTextStyle.B1,
                color = GrayColor.C300,
                modifier =
                    Modifier
                        .weight(1f)
                        .then(onOpenShopDetail?.let { Modifier.noRippleClickable(onClick = it) } ?: Modifier),
            )
        }
        ReviewCardOverflowMenu(
            review = review,
            actions = actions,
            actionsEnabled = actionsEnabled,
        )
    }
}
