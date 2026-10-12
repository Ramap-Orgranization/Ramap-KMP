package com.peto.ramap.designsystem.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop

@Composable
fun ShopReviewCard(
    review: Review,
    shop: RamenShop?,
    onOpenShopDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    shopName: String? = null,
    actions: ReviewCardActions = ReviewCardActions(),
    actionsEnabled: Boolean = true,
    photoViewerEnabled: Boolean = true,
) {
    val onShopClick =
        review.shopId.takeIf { it.isNotBlank() }?.let { shopId ->
            { onOpenShopDetail(shopId) }
        }
    ReviewCard(
        review = review,
        modifier = modifier,
        actions = actions.copy(onReviewClick = onShopClick),
        actionsEnabled = actionsEnabled,
        photoViewerEnabled = photoViewerEnabled,
        header = {
            ShopReviewCardHeader(
                review = review,
                shop = shop,
                shopName = shopName,
                onOpenShopDetail = onShopClick.takeIf { actionsEnabled },
                actions = actions,
                actionsEnabled = actionsEnabled,
            )
        },
    )
}
