package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto

class MyReviewPreviewParameterProvider : PreviewParameterProvider<MyReview> {
    val myReviewPreviewSamples =
        listOf(
            MyReview(
                id = "1",
                shopId = "shop1",
                shopName = "멘야준 본점",
                body = "시오라멘 국물이 너무 깔끔하고 차슈가 부드러웠습니다. 재방문 의사 있습니다!",
                createdAt = "2026-09-28T12:00:00Z",
                images = listOf(MyReviewPhoto("1", "url1"), MyReviewPhoto("2", "url2")),
                isPublic = true,
                moderationStatus = ReviewModerationStatus.PUBLISHED,
            ),
            MyReview(
                id = "2",
                shopId = "shop2",
                shopName = "라멘지라이",
                body = "나만 알고 싶은 비공개 라멘 메모입니다.",
                createdAt = "2026-09-27T10:00:00Z",
                images = emptyList(),
                isPublic = false,
                moderationStatus = ReviewModerationStatus.PUBLISHED,
            ),
            MyReview(
                id = "3",
                shopId = "shop3",
                shopName = "오레노라멘",
                body = "검토 중인 리뷰입니다.",
                createdAt = "2026-09-29T15:00:00Z",
                images = emptyList(),
                isPublic = true,
                moderationStatus = ReviewModerationStatus.PENDING,
            ),
        )

    override val values: Sequence<MyReview> = myReviewPreviewSamples.asSequence()
}
