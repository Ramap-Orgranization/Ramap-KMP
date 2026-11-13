package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewModerationStatus
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshReviewListUseCaseTest {
    private val refresh = RefreshReviewListUseCase(ReviewChangeReducer())
    private val publicReview = Review("review", "shop", "라멘 리뷰", "2026-10-14")

    @Test
    fun `비로그인에서 로그인할 때 공개된 리뷰만 잠시 유지한다`() {
        val rows =
            listOf(
                publicReview,
                publicReview.copy(id = "private", isPublic = false),
                publicReview.copy(id = "pending", moderationStatus = ReviewModerationStatus.PENDING),
                publicReview.copy(id = "blocked", isBlocked = true),
            )

        assertEquals(listOf(publicReview), refresh.retainGuestReviews(null, "viewer", rows) { it })
    }

    @Test
    fun `로그아웃과 계정 변경에서는 이전 계정의 목록을 유지하지 않는다`() {
        val rows = listOf(publicReview)

        assertTrue(refresh.retainGuestReviews("viewer", null, rows) { it }.isEmpty())
        assertTrue(refresh.retainGuestReviews("viewer", "another", rows) { it }.isEmpty())
    }
}
