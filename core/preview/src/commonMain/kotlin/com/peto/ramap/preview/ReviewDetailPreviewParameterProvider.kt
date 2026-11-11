package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread

class ReviewDetailPreviewParameterProvider : PreviewParameterProvider<ReviewDetail> {
    val sampleThreads =
        listOf(
            ReviewCommentThread(
                comment =
                    ReviewComment(
                        id = "comment-1",
                        parentId = null,
                        body = "정말 맛있어 보이네요! 다음에 방문해보겠습니다.",
                        nickname = "라멘러버",
                        createdAt = "2026-02-15T13:00:00Z",
                        canReport = true,
                        canManage = false,
                    ),
                replies =
                    listOf(
                        ReviewComment(
                            id = "comment-2",
                            parentId = "comment-1",
                            body = "추천드립니다!",
                            nickname = "면발수집가",
                            createdAt = "2026-02-15T14:00:00Z",
                            canReport = false,
                            canManage = true,
                        ),
                    ),
                nextReplyCursor = null,
            ),
        )

    val reviewDetailPreviewSamples =
        listOf(
            ReviewDetail(
                review = ReviewPreviewParameterProvider().reviewPreviewSamples.first(),
                shopName = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first().name,
                canLike = true,
                canWriteComments = true,
                shop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first(),
                canShare = true,
                reviewNotificationsEnabled = true,
            ),
        )

    override val values: Sequence<ReviewDetail> = reviewDetailPreviewSamples.asSequence()
}
