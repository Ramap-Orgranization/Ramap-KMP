package com.peto.ramap.ui.shop.review

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import com.peto.ramap.ui.shop.review.contract.ReviewsSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_review_like_description
import ramap.shared.generated.resources.login_required_review_report_description
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_report_failed
import ramap.shared.generated.resources.review_unblock_failed
import ramap.shared.generated.resources.review_unblock_success
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsFeedbackTest {
    @Test
    fun `비로그인 사용자의 좋아요와 신고에는 각각의 로그인 이유를 전달한다`() =
        coroutinesTest {
            val fixture = ReviewsViewModelFixture()
            try {
                fixture.reviews.reviews = listOf(fixture.otherReview)
                fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
                runCurrent()
                fixture.viewModel.sideEffect.test {
                    fixture.viewModel.dispatch(ReviewsIntent.OnReviewLikeToggled(fixture.otherReview))
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowLoginGuide(Res.string.login_required_review_like_description),
                        awaitItem(),
                    )
                    fixture.viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(fixture.otherReview))
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowLoginGuide(Res.string.login_required_review_report_description),
                        awaitItem(),
                    )
                    assertEquals(emptyList(), fixture.likeRequests)
                    assertEquals(null, fixture.viewModel.uiState.value.reportReview)
                }
            } finally {
                fixture.store.clear()
            }
        }

    @Test
    fun `차단 해제와 신고가 성공하면 각각의 작업 결과를 안내한다`() =
        coroutinesTest {
            val moderation = FakeUserModerationRepository()
            val fixture = ReviewsViewModelFixture(moderation)
            try {
                openReviews(fixture)
                val viewModel = fixture.viewModel
                val blocked = fixture.otherReview.copy(isBlocked = true)
                viewModel.sideEffect.test {
                    fixture.reviews.reviews = listOf(fixture.otherReview)
                    viewModel.dispatch(ReviewsIntent.OnBlockedReviewUnblockRequested(blocked))
                    viewModel.dispatch(ReviewsIntent.OnBlockedReviewUnblockConfirmed)
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowToast(Res.string.review_unblock_success),
                        awaitItem(),
                    )
                    assertEquals(listOf(blocked.author.userId), moderation.unblockedUserIds)

                    viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(fixture.otherReview))
                    viewModel.dispatch(ReviewsIntent.OnReviewReportSubmitted(ReportReason.SPAM, ""))
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowToast(Res.string.review_action_success, ToastType.SUCCESS),
                        awaitItem(),
                    )
                }
            } finally {
                fixture.store.clear()
            }
        }

    @Test
    fun `차단 해제와 신고가 실패하면 조회 오류 대신 실패한 작업을 안내한다`() =
        coroutinesTest {
            val failure = RamapResult.Error(RamapError.Http(500))
            val moderation =
                object : UserModerationRepository by FakeUserModerationRepository() {
                    override suspend fun unblockUser(userId: String): RamapResult<Unit> = failure

                    override suspend fun reportReview(
                        reviewId: String,
                        reason: ReportReason,
                        details: String,
                    ): RamapResult<Unit> = failure
                }
            val fixture = ReviewsViewModelFixture(moderation)
            try {
                openReviews(fixture)
                val viewModel = fixture.viewModel
                val blocked = fixture.otherReview.copy(isBlocked = true)
                viewModel.sideEffect.test {
                    viewModel.dispatch(ReviewsIntent.OnBlockedReviewUnblockRequested(blocked))
                    viewModel.dispatch(ReviewsIntent.OnBlockedReviewUnblockConfirmed)
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowToast(Res.string.review_unblock_failed, ToastType.ERROR),
                        awaitItem(),
                    )
                    assertEquals(listOf(blocked), viewModel.uiState.value.reviews)

                    viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(blocked))
                    viewModel.dispatch(ReviewsIntent.OnReviewReportSubmitted(ReportReason.SPAM, ""))
                    runCurrent()
                    assertEquals(
                        ReviewsSideEffect.ShowToast(Res.string.review_report_failed, ToastType.ERROR),
                        awaitItem(),
                    )
                    assertEquals(blocked, viewModel.uiState.value.reportReview)
                }
            } finally {
                fixture.store.clear()
            }
        }

    private fun TestScope.openReviews(fixture: ReviewsViewModelFixture) {
        fixture.changes.reset("owner")
        fixture.profile.sessionUserId.value = "owner"
        fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
        fixture.reviews.reviews = listOf(fixture.otherReview.copy(isBlocked = true))
        fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
        runCurrent()
    }
}
