package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.shop.ShopReview
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.shop_review_empty_description
import ramap.shared.generated.resources.shop_review_empty_title
import ramap.shared.generated.resources.shop_review_go_write

@Composable
internal fun ShopReviewsContent(
    shopName: String,
    reviews: List<ShopReview>,
    onOpenProfile: (String) -> Unit,
    onWriteReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (reviews.isEmpty()) {
            LoadErrorContent(
                image = Res.drawable.laduck_error_confused,
                title = stringResource(Res.string.shop_review_empty_title),
                description =
                    stringResource(
                        Res.string.shop_review_empty_description,
                        shopName,
                    ),
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ReviewWriteAction(
                onClick = onWriteReviewClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ReviewWriteAction(
                onClick = onWriteReviewClick,
                modifier = Modifier.align(Alignment.End),
            )
            reviews.forEach { review ->
                ReviewCard(
                    review = review,
                    onOpenProfile = onOpenProfile,
                )
            }
        }
    }
}

@Composable
private fun ReviewWriteAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppText(
        text = stringResource(Res.string.shop_review_go_write),
        style = AppTextStyle.T3,
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        color = GrayColor.C300,
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 5.dp)
                .noRippleClickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun ShopReviewsContentPreview() {
    RamapTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            ShopReviewsContent(
                shopName = "멘야 하나비",
                reviews = emptyList(),
                onOpenProfile = {},
                onWriteReviewClick = {},
            )
            ShopReviewsContent(
                shopName = "멘야 하나비",
                reviews =
                    listOf(
                        ShopReview(
                            id = "preview-review-1",
                            shopId = "preview-shop",
                            body = "국물이 진하고 면발의 식감이 아주 좋습니다. 또 방문하고 싶네요!",
                            createdAt = "2026-02-15T12:00:00Z",
                            imageUrls = emptyList(),
                            author = ReviewAuthor("preview-author-1", "면발수집가"),
                        ),
                        ShopReview(
                            id = "preview-review-2",
                            shopId = "preview-shop",
                            body = "자가제면이라 면발이 탱탱해요.",
                            createdAt = "2026-02-14T18:30:00Z",
                            imageUrls = emptyList(),
                            author = ReviewAuthor("preview-author-2", "라멘러버"),
                        ),
                    ),
                onOpenProfile = {},
                onWriteReviewClick = {},
            )
        }
    }
}
