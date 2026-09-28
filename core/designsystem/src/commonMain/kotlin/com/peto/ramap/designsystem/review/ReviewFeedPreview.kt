package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.shop.ShopReview
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.community_guidelines_title
import ramap.shared.generated.resources.review_feed_description
import ramap.shared.generated.resources.shop_review_title
import ramap.shared.generated.resources.shop_review_write

@Preview(name = "Review feed", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Review feed large text", widthDp = 320, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
fun ReviewFeedPreview() {
    RamapTheme {
        ReviewPage(
            title = stringResource(Res.string.shop_review_title),
            onBack = {},
            action = {
                TextButton(
                    onClick = {},
                ) {
                    AppText(
                        text = stringResource(Res.string.community_guidelines_title),
                        style = AppTextStyle.B3,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    ReviewPixelHeader(
                        title = stringResource(Res.string.shop_review_title),
                        description = stringResource(Res.string.review_feed_description),
                    )
                }
                item {
                    ReviewCard(
                        review =
                            ShopReview(
                                id = "preview-review-1",
                                shopId = "preview-shop",
                                body = "국물 한 입에 기분이 좋아지는 라멘. 쫄깃한 면과 부드러운 차슈가 잘 어울렸어요.",
                                createdAt = "2026-09-24T12:00:00Z",
                                author = ReviewAuthor("preview-author-1", "면발수집가"),
                            ),
                        onOpenProfile = {},
                        onReport = {},
                    )
                }
                item {
                    ReviewCard(
                        review =
                            ShopReview(
                                id = "preview-review-2",
                                shopId = "preview-shop",
                                body = "마지막 한 입까지 따뜻하게 먹었어요. 다음에는 다른 메뉴도 먹어보려고요.",
                                createdAt = "2026-09-23T12:00:00Z",
                                author = ReviewAuthor("preview-author-2", "오늘도라멘"),
                            ),
                        onOpenProfile = {},
                    )
                }
            }
            Surface(
                shadowElevation = 4.dp,
            ) {
                AppButton(
                    text = stringResource(Res.string.shop_review_write),
                    onClick = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    cornerRadius = 0.dp,
                )
            }
        }
    }
}
