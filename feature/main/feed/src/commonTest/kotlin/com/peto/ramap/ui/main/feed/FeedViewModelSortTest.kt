package com.peto.ramap.ui.main.feed

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelSortTest {
    @Test
    fun `정렬을 바꾸면 이전 목록과 커서를 지우고 새 정렬의 첫 페이지를 표시한다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<ReviewFeedPage>>()
            val feed =
                FakeReviewFeedRepository().apply {
                    response = { _, sort ->
                        if (sort == ReviewFeedSort.POPULAR) pending.await() else page("latest", next = cursor())
                    }
                }
            val viewModel = createViewModel(feed)
            runCurrent()
            assertEquals(
                "latest",
                viewModel.uiState.value.reviews
                    .single()
                    .review.id,
            )

            viewModel.dispatch(FeedIntent.SelectSort(ReviewFeedSort.POPULAR))
            runCurrent()
            assertEquals(ReviewFeedSort.POPULAR, viewModel.uiState.value.sort)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.nextCursor)
            assertFalse(viewModel.uiState.value.hasLoaded)

            pending.complete(page("popular"))
            runCurrent()
            assertEquals(
                "popular",
                viewModel.uiState.value.reviews
                    .single()
                    .review.id,
            )
            assertEquals(listOf<Pair<ReviewFeedSort, ReviewFeedCursor?>>(ReviewFeedSort.LATEST to null, ReviewFeedSort.POPULAR to null), feed.requests)
        }

    @Test
    fun `이전 정렬의 추가 조회가 취소를 무시하고 끝나도 새 정렬 목록에 섞이지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<ReviewFeedPage>>()
            val feed =
                FakeReviewFeedRepository().apply {
                    response = { next, sort ->
                        when {
                            sort == ReviewFeedSort.POPULAR -> page("popular")
                            next != null -> withContext(NonCancellable) { pending.await() }
                            else -> page("latest", next = cursor())
                        }
                    }
                }
            val viewModel = createViewModel(feed)
            runCurrent()
            viewModel.dispatch(FeedIntent.LoadMore)
            runCurrent()
            viewModel.dispatch(FeedIntent.SelectSort(ReviewFeedSort.POPULAR))
            runCurrent()
            pending.complete(page("old-append"))
            runCurrent()

            assertEquals(
                listOf("popular"),
                viewModel.uiState.value.reviews
                    .map { it.review.id },
            )
            assertFalse(viewModel.uiState.value.appending)
        }

    @Test
    fun `인기순 추가 조회는 좋아요 수가 포함된 다음 커서를 전달한다`() =
        coroutinesTest {
            val popularCursor = cursor().copy(likeCount = 12)
            val feed =
                FakeReviewFeedRepository().apply {
                    response = { next, sort ->
                        when {
                            sort == ReviewFeedSort.LATEST -> page("latest")
                            next == null -> page("popular-first", next = popularCursor)
                            else -> page("popular-next")
                        }
                    }
                }
            val viewModel = createViewModel(feed)
            runCurrent()
            viewModel.dispatch(FeedIntent.SelectSort(ReviewFeedSort.POPULAR))
            runCurrent()
            viewModel.dispatch(FeedIntent.LoadMore)
            runCurrent()

            assertEquals(ReviewFeedSort.POPULAR to popularCursor, feed.requests.last())
            assertEquals(
                listOf("popular-first", "popular-next"),
                viewModel.uiState.value.reviews
                    .map { it.review.id },
            )
            assertNull(viewModel.uiState.value.nextCursor)
        }

    @Test
    fun `인기순에서 좋아요가 바뀌면 서버 순서를 다시 조회한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            var updated = false
            val feed =
                FakeReviewFeedRepository().apply {
                    response = { _, _ -> if (updated) page("new-top", "previous-top") else page("previous-top", "new-top") }
                }
            val viewModel = createViewModel(feed, changes = changes)
            runCurrent()
            viewModel.dispatch(FeedIntent.SelectSort(ReviewFeedSort.POPULAR))
            runCurrent()
            updated = true
            changes.publish(ReviewChange.Liked("new-top", ReviewLike(10, true)))
            runCurrent()

            assertEquals(
                listOf("new-top", "previous-top"),
                viewModel.uiState.value.reviews
                    .map { it.review.id },
            )
            assertEquals(3, feed.requests.size)
        }

    @Test
    fun `계정을 바꿔도 선택한 정렬을 유지하며 새 세션으로 다시 조회한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository("owner")
            val changes = FakeReviewChangeStore()
            val feed = FakeReviewFeedRepository()
            val viewModel = createViewModel(feed, profiles, changes)
            runCurrent()
            viewModel.dispatch(FeedIntent.SelectSort(ReviewFeedSort.POPULAR))
            runCurrent()
            profiles.sessionUserId.value = "another"
            changes.reset("another")
            runCurrent()

            assertEquals("another", viewModel.uiState.value.currentUserId)
            assertEquals(ReviewFeedSort.POPULAR, viewModel.uiState.value.sort)
            assertEquals(ReviewFeedSort.POPULAR to null, feed.requests.last())
        }

    private fun createViewModel(
        feed: FakeReviewFeedRepository,
        profiles: FakeProfileRepository = FakeProfileRepository("owner"),
        changes: FakeReviewChangeStore = FakeReviewChangeStore(),
    ) = FeedViewModel(
        reviewFeedRepository = feed,
        userModerationRepository = FakeUserModerationRepository(),
        profilesRepository = profiles,
        reviews = FakeShopReviewRepository(),
        changes = changes,
        reducer = ReviewChangeReducer(),
    )

    private fun page(
        vararg ids: String,
        next: ReviewFeedCursor? = null,
    ): RamapResult<ReviewFeedPage> =
        RamapResult.Success(
            ReviewFeedPage(
                reviews = ids.map { id -> ProfileReview(Review(id, "shop", "리뷰 본문", "2026-10-26T00:00:00Z"), ramenShopFixture(id = "shop")) },
                nextCursor = next,
            ),
        )

    private fun cursor() = ReviewFeedCursor("2026-10-26T00:00:00Z", "cursor-review")
}
