package com.peto.ramap.ui.main.feed

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelLikeTest {
    @Test
    fun `피드에서 내 공개 리뷰에 좋아요를 누르고 취소할 수 있다`() =
        coroutinesTest {
            val review = Review("review", "shop", "맛있는 라멘", "2026-11-11", author = ReviewAuthor("owner", "작성자"))
            val row = ProfileReview(review, ramenShopFixture(id = "shop"))
            val feed = FakeReviewFeedRepository().apply { response = { _, _ -> RamapResult.Success(ReviewFeedPage(listOf(row), null)) } }
            val likeRequests = mutableListOf<Pair<String, Boolean>>()
            val reviews =
                object : ReviewRepository by FakeShopReviewRepository() {
                    override suspend fun setReviewLike(
                        reviewId: String,
                        liked: Boolean,
                    ): RamapResult<ReviewLike> {
                        likeRequests += reviewId to liked
                        return RamapResult.Success(ReviewLike(if (liked) 1 else 0, liked))
                    }
                }
            val viewModel =
                FeedViewModel(
                    reviewFeedRepository = feed,
                    userModerationRepository = FakeUserModerationRepository(),
                    profilesRepository = FakeProfileRepository("owner"),
                    reviews = reviews,
                    changes = FakeReviewChangeStore(),
                    reducer = ReviewChangeReducer(),
                )
            runCurrent()
            assertTrue(viewModel.uiState.value.allowsLike(review))

            viewModel.dispatch(FeedIntent.ToggleLike(review))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.reviews
                    .single()
                    .review.isLiked,
            )

            viewModel.dispatch(FeedIntent.ToggleLike(review))
            runCurrent()
            assertFalse(
                viewModel.uiState.value.reviews
                    .single()
                    .review.isLiked,
            )
            assertEquals(listOf(review.id to true, review.id to false), likeRequests)
        }
}
