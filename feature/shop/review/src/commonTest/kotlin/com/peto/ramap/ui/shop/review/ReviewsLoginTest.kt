package com.peto.ramap.ui.shop.review

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsLoginTest {
    @Test
    fun `인증이 사용자 ID보다 먼저 도착해도 로그인 후 리뷰 권한과 동작을 갱신한다`() = loginScenario(authenticatedFirst = true)

    @Test
    fun `사용자 ID가 인증보다 먼저 도착해도 로그인 후 리뷰 권한과 동작을 갱신한다`() = loginScenario(authenticatedFirst = false)

    @Test
    fun `리뷰 변경 저장소가 사용자 ID보다 먼저 초기화되어도 로그인 조회를 완료한다`() =
        reviewScenario { fixture ->
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.changes.reset("owner")
            runCurrent()
            assertEquals(1, fixture.pageRequests)
            assertEquals(0, fixture.profileRequests)
            fixture.reviews.reviews = listOf(fixture.ownReview)
            authenticate(fixture)
            assertEquals(2, fixture.pageRequests)
            assertEquals(1, fixture.profileRequests)
            assertEquals("owner", fixture.viewModel.uiState.value.currentUserId)
            assertEquals(listOf(fixture.ownReview), fixture.viewModel.uiState.value.reviews)
            assertFalse(fixture.viewModel.uiState.value.isLoading)
            assertFalse(fixture.viewModel.uiState.value.hasReviewLoadFailure)
        }

    @Test
    fun `로그인 조회 전 변경은 다시 적용하지 않고 이후 변경부터 반영한다`() =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.changes.reset("owner")
            fixture.changes.publish(
                ReviewChange.Written(
                    reviewId = fixture.ownReview.id,
                    userId = "owner",
                    shopId = "shop",
                    before = ReviewSnapshot(fixture.ownReview, "매장"),
                    after = null,
                    counts = ReviewOwnerCounts(0, 0, 0, 0, true),
                ),
            )
            runCurrent()
            authenticate(fixture)
            fixture.changes.publish(ReviewChange.Liked("other", ReviewLike(3, true)))
            runCurrent()
            assertEquals(1, fixture.viewModel.uiState.value.reviewCount)
            assertEquals(listOf(fixture.otherReview.copy(likeCount = 3, isLiked = true)), fixture.viewModel.uiState.value.reviews)
            assertEquals(2, fixture.pageRequests)
        }

    @Test
    fun `로그인 후 늦게 도착한 비로그인 리뷰 성공 응답은 무시한다`() = lateGuestResponseScenario(fails = false)

    @Test
    fun `로그인 후 늦게 도착한 비로그인 리뷰 실패 응답은 무시한다`() = lateGuestResponseScenario(fails = true)

    @Test
    fun `로그인 저장소 반영을 기다리는 중에도 이전 리뷰 성공 응답은 무시한다`() = lateGuestResponseScenario(fails = false, completeBeforeJournal = true)

    @Test
    fun `로그인 저장소 반영을 기다리는 중에도 이전 리뷰 실패 응답은 무시한다`() = lateGuestResponseScenario(fails = true, completeBeforeJournal = true)

    @Test
    fun `상세 저장소를 비우면 늦게 도착한 리뷰 성공 응답은 무시한다`() = clearedResponseScenario(fails = false)

    @Test
    fun `상세 저장소를 비우면 늦게 도착한 리뷰 실패 응답은 무시한다`() = clearedResponseScenario(fails = true)

    @Test
    fun `상세 저장소를 비우면 늦게 도착한 프로필은 반영하지 않는다`() =
        reviewScenario { fixture ->
            val response = CompletableDeferred<RamapResult<AccountProfile>>()
            var returned = false
            fixture.fetchProfile = { withContext(NonCancellable) { response.await() }.also { returned = true } }
            try {
                fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
                runCurrent()
                authenticate(fixture)
                assertEquals(1, fixture.profileRequests)
                fixture.store.clear()
                response.complete(RamapResult.Success(AccountProfile("owner", "나", isPublic = false)))
                runCurrent()
                assertTrue(returned)
                assertNull(fixture.viewModel.uiState.value.currentProfileIsPublic)
                assertFalse(fixture.viewModel.viewModelScope.isActive)
            } finally {
                response.complete(RamapResult.Error(RamapError.Http(500)))
            }
        }

    @Test
    fun `상세를 닫으면 예약된 로그인과 이후 리뷰 변경을 처리하지 않는다`() =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
            fixture.profile.sessionUserId.value = "owner"
            fixture.store.clear()
            fixture.changes.publish(ReviewChange.Liked("other", ReviewLike(3, true)))
            fixture.viewModel.dispatch(ReviewsIntent.OnRetry)
            runCurrent()
            assertEquals(1, fixture.pageRequests)
            assertEquals(0, fixture.profileRequests)
            assertEquals(listOf(fixture.otherReview), fixture.viewModel.uiState.value.reviews)
            assertNull(fixture.viewModel.uiState.value.currentUserId)
            assertFalse(fixture.viewModel.viewModelScope.isActive)
        }

    @Test
    fun `변경 저장소의 로그인 반영을 기다리다 상세를 닫으면 조회를 시작하지 않는다`() =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
            fixture.profile.sessionUserId.value = "owner"
            runCurrent()
            assertEquals(1, fixture.pageRequests)
            assertEquals(0, fixture.profileRequests)
            fixture.store.clear()
            fixture.changes.reset("owner")
            runCurrent()
            assertEquals(1, fixture.pageRequests)
            assertEquals(0, fixture.profileRequests)
            assertEquals(listOf(fixture.otherReview), fixture.viewModel.uiState.value.reviews)
            assertNull(fixture.viewModel.uiState.value.currentUserId)
            assertFalse(fixture.viewModel.viewModelScope.isActive)
        }

    @Test
    fun `로그인 후에도 같은 계정의 좋아요와 리뷰 수정 및 삭제를 반영한다`() =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview, fixture.ownReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            authenticate(fixture)
            val requests = fixture.pageRequests
            fixture.changes.publish(ReviewChange.Liked("other", ReviewLike(3, true)))
            val edited = fixture.ownReview.copy(body = "다시 먹고 싶은 라멘", isPublic = true)
            val written =
                ReviewChange.Written(
                    reviewId = edited.id,
                    userId = "owner",
                    shopId = "shop",
                    before = ReviewSnapshot(fixture.ownReview, "매장"),
                    after = ReviewSnapshot(edited, "매장"),
                    counts = ReviewOwnerCounts(1, 1, 0, 1, true),
                )
            fixture.changes.publish(written)
            runCurrent()
            assertEquals(
                edited.copy(author = edited.author.copy(reviewCount = 1)),
                fixture.viewModel.uiState.value.reviews
                    .first { it.id == "mine" },
            )
            assertEquals(true, fixture.viewModel.uiState.value.currentProfileIsPublic)
            assertEquals(
                3,
                fixture.viewModel.uiState.value.reviews
                    .first { it.id == "other" }
                    .likeCount,
            )

            fixture.changes.publish(written.copy(before = written.after, after = null, counts = ReviewOwnerCounts(0, 0, 0, 0, true)))
            runCurrent()
            assertEquals(
                listOf("other"),
                fixture.viewModel.uiState.value.reviews
                    .map { it.id },
            )
            assertEquals(1, fixture.viewModel.uiState.value.reviewCount)
            fixture.changes.publish(ReviewChange.Liked("other", ReviewLike(4, true)))
            runCurrent()
            assertEquals(1, fixture.viewModel.uiState.value.reviewCount)
            assertEquals(
                4,
                fixture.viewModel.uiState.value.reviews
                    .single()
                    .likeCount,
            )
            assertEquals(requests, fixture.pageRequests)
        }

    @Test
    fun `로그인 후 좋아요 저장이 실패하면 낙관적으로 바꾼 표시를 되돌린다`() =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            authenticate(fixture)
            val response = CompletableDeferred<RamapResult<ReviewLike>>()
            fixture.updateLike = { response.await() }
            fixture.viewModel.dispatch(ReviewsIntent.OnReviewLikeToggled(fixture.otherReview))
            runCurrent()
            assertTrue(
                fixture.viewModel.uiState.value.reviews
                    .single()
                    .isLiked,
            )
            response.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            assertEquals(
                fixture.otherReview,
                fixture.viewModel.uiState.value.reviews
                    .single(),
            )
            assertNull(fixture.viewModel.uiState.value.actingReviewId)
        }

    private fun loginScenario(authenticatedFirst: Boolean) =
        reviewScenario { fixture ->
            fixture.reviews.reviews = listOf(fixture.otherReview)
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.viewModel.dispatch(ReviewsIntent.OnReviewLikeToggled(fixture.otherReview))
            fixture.viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(fixture.otherReview))
            runCurrent()
            assertTrue(fixture.likeRequests.isEmpty())
            assertNull(fixture.viewModel.uiState.value.reportReview)
            assertFalse(fixture.ownReview.allowsDeletionBy(fixture.viewModel.uiState.value.currentUserId))

            fixture.reviews.reviews = listOf(fixture.ownReview, fixture.otherReview)
            if (authenticatedFirst) fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED) else fixture.profile.sessionUserId.value = "owner"
            runCurrent()
            if (authenticatedFirst) fixture.profile.sessionUserId.value = "owner" else fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            fixture.viewModel.dispatch(ReviewsIntent.OnRetry)
            runCurrent()
            assertEquals(1, fixture.pageRequests)
            assertEquals(0, fixture.profileRequests)
            assertNull(fixture.viewModel.uiState.value.currentUserId)
            fixture.changes.reset("owner")
            runCurrent()
            val state = fixture.viewModel.uiState.value
            assertTrue(state.isLoggedIn)
            assertEquals(false, state.currentProfileIsPublic)
            assertEquals(listOf(fixture.ownReview, fixture.otherReview), state.reviews)
            assertTrue(state.reviews.first().allowsDeletionBy(state.currentUserId))
            assertEquals(2, fixture.pageRequests)
            assertEquals(1, fixture.profileRequests)
            assertEquals(0, fixture.profile.sessionUserId.subscriptionCount.value)

            fixture.viewModel.dispatch(ReviewsIntent.OnReviewLikeToggled(fixture.otherReview))
            fixture.viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(fixture.otherReview))
            runCurrent()
            assertEquals(listOf("other" to true), fixture.likeRequests)
            assertTrue(
                fixture.viewModel.uiState.value.reviews
                    .first { it.id == "other" }
                    .isLiked,
            )
            assertEquals(fixture.otherReview, fixture.viewModel.uiState.value.reportReview)
        }

    private fun lateGuestResponseScenario(
        fails: Boolean,
        completeBeforeJournal: Boolean = false,
    ) = reviewScenario { fixture ->
        val response = CompletableDeferred<RamapResult<ShopReviewsPage>>()
        var returned = false
        fixture.fetchPage = { _, _ -> withContext(NonCancellable) { response.await() }.also { returned = true } }
        val oldResult = if (fails) RamapResult.Error(RamapError.Http(500)) else RamapResult.Success(ShopReviewsPage(emptyList(), 0))
        try {
            fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fixture.fetchPage = { _, _ -> RamapResult.Success(ShopReviewsPage(listOf(fixture.ownReview), 1)) }
            if (completeBeforeJournal) {
                fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
                fixture.profile.sessionUserId.value = "owner"
                runCurrent()
                response.complete(oldResult)
                runCurrent()
                assertTrue(returned)
                assertFalse(fixture.viewModel.uiState.value.hasLoaded)
                assertFalse(fixture.viewModel.uiState.value.hasReviewLoadFailure)
                assertEquals(1, fixture.pageRequests)
                assertEquals(0, fixture.profileRequests)
            }
            authenticate(fixture)
            response.complete(oldResult)
            runCurrent()
            assertTrue(returned)
            assertEquals(listOf(fixture.ownReview), fixture.viewModel.uiState.value.reviews)
            assertFalse(fixture.viewModel.uiState.value.isLoading)
            assertFalse(fixture.viewModel.uiState.value.hasReviewLoadFailure)
        } finally {
            response.complete(oldResult)
        }
    }

    private fun clearedResponseScenario(fails: Boolean) =
        reviewScenario { fixture ->
            val response = CompletableDeferred<RamapResult<ShopReviewsPage>>()
            var returned = false
            fixture.fetchPage = { _, _ -> withContext(NonCancellable) { response.await() }.also { returned = true } }
            val result = if (fails) RamapResult.Error(RamapError.Http(500)) else RamapResult.Success(ShopReviewsPage(listOf(fixture.otherReview), 1))
            try {
                fixture.viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
                runCurrent()
                fixture.store.clear()
                runCurrent()
                response.complete(result)
                runCurrent()
                assertTrue(returned)
                assertTrue(
                    fixture.viewModel.uiState.value.reviews
                        .isEmpty(),
                )
                assertFalse(fixture.viewModel.uiState.value.hasLoaded)
                assertFalse(fixture.viewModel.uiState.value.hasReviewLoadFailure)
                assertFalse(fixture.viewModel.viewModelScope.isActive)
            } finally {
                response.complete(result)
            }
        }

    private fun TestScope.authenticate(fixture: ReviewsViewModelFixture) {
        fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
        fixture.profile.sessionUserId.value = "owner"
        if (fixture.changes.journal.value.userId != "owner") fixture.changes.reset("owner")
        runCurrent()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun reviewScenario(block: suspend TestScope.(ReviewsViewModelFixture) -> Unit) =
    coroutinesTest {
        val fixture = ReviewsViewModelFixture()
        try {
            block(fixture)
        } finally {
            fixture.store.clear()
        }
    }
