package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review

class ReviewPreviewParameterProvider : PreviewParameterProvider<Review> {
    val reviewPreviewSamples =
        listOf(
            Review(
                id = "review-1",
                shopId = "shop-1",
                body = "국물이 아주 진하고 진한 돈코츠 라멘입니다. 차슈도 부드럽고 면발의 익힘 정도가 딱 완벽했어요!",
                createdAt = "2026-02-15T12:00:00Z",
                visitNumber = 3,
                likeCount = 12,
                isLiked = true,
                author =
                    ReviewAuthor(
                        userId = "user-1",
                        nickname = "면발수집가",
                        reviewCount = 15,
                    ),
            ),
            Review(
                id = "review-2",
                shopId = "shop-1",
                body = "자가제면이라 면발이 탱탱하고 맛있어요.",
                createdAt = "2026-02-14T18:30:00Z",
                visitNumber = 1,
                likeCount = 5,
                isLiked = false,
                author =
                    ReviewAuthor(
                        userId = "my-user-id",
                        nickname = "나의닉네임",
                        reviewCount = 4,
                    ),
            ),
            Review(
                id = "review-3",
                shopId = "shop-1",
                body = "검토 중인 리뷰입니다.",
                createdAt = "2026-02-13T10:00:00Z",
                visitNumber = 1,
                moderationStatus = ReviewModerationStatus.PENDING,
                author =
                    ReviewAuthor(
                        userId = "user-2",
                        nickname = "라멘마니아",
                        reviewCount = 2,
                    ),
            ),
        )

    override val values: Sequence<Review> = reviewPreviewSamples.asSequence()
}
