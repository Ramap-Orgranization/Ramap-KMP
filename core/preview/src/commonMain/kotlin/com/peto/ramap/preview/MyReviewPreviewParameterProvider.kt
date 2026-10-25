package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review

class MyReviewPreviewParameterProvider : PreviewParameterProvider<MyReview> {
    val myReviewPreviewSamples =
        listOf(
            MyReview(
                review =
                    Review(
                        id = "1",
                        shopId = "shop1",
                        body = "시오라멘 국물이 너무 깔끔하고 차슈가 부드러웠습니다. 재방문 의사 있습니다!",
                        createdAt = "2026-09-28T12:00:00Z",
                        images = listOf(MyReviewPhoto("1", "url1"), MyReviewPhoto("2", "url2")),
                        imageUrls = listOf("1", "2"),
                        isPublic = true,
                        moderationStatus = ReviewModerationStatus.PUBLISHED,
                    ),
                shopName = "멘야준 본점",
            ),
            MyReview(
                review =
                    Review(
                        id = "2",
                        shopId = "shop2",
                        body = "나만 알고 싶은 비공개 라멘 메모입니다.",
                        createdAt = "2026-09-27T10:00:00Z",
                        images = emptyList(),
                        isPublic = false,
                        moderationStatus = ReviewModerationStatus.PUBLISHED,
                    ),
                shopName = "라멘지라이",
            ),
            MyReview(
                review =
                    Review(
                        id = "3",
                        shopId = "shop3",
                        body = "검토 중인 리뷰입니다.",
                        createdAt = "2026-09-29T15:00:00Z",
                        images = emptyList(),
                        isPublic = true,
                        moderationStatus = ReviewModerationStatus.PENDING,
                    ),
                shopName = "오레노라멘",
            ),
        )

    override val values: Sequence<MyReview> = myReviewPreviewSamples.asSequence()
}
