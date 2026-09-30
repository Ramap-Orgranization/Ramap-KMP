package com.peto.ramap.ui.review.other

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OtherReviewsViewModelTest {
    @Test
    fun `guest block request opens login without blocking the author`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(userId = null))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
                assertEquals(OtherReviewsEffect.LoginRequired, awaitItem())
            }
            assertTrue(community.blockedUserIds.isEmpty())
        }

    @Test
    fun `profile reviews load in pages and stop at the end`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = (1..23).map(::review)
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            assertEquals(
                "라멘팬",
                viewModel.uiState.value.profile
                    ?.nickname,
            )
            assertEquals(20, viewModel.uiState.value.reviews.size)
            assertTrue(viewModel.uiState.value.hasMore)

            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()
            assertEquals(listOf("author"), community.requestedProfileUserIds)
            assertEquals(listOf(0L, 20L), community.requestedOffsets)
            assertEquals(23, viewModel.uiState.value.reviews.size)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `unavailable profile does not request reviews`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.profile = null
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.loaded)
            assertEquals(null, viewModel.uiState.value.profile)
            assertFalse(viewModel.uiState.value.isPrivate)
            assertTrue(community.requestedOffsets.isEmpty())
        }

    @Test
    fun `private profile hides identity and reviews`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.profilePrivate = true
            community.reviews = listOf(review(1))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.loaded)
            assertTrue(viewModel.uiState.value.isPrivate)
            assertEquals(null, viewModel.uiState.value.profile)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertTrue(community.requestedOffsets.isEmpty())
        }

    @Test
    fun `blocked profile keeps identity but hides reviews`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.blockedUserIds += "author"
            community.reviews = listOf(review(1))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.isBlocked)
            assertEquals(listOf("author"), community.requestedProfileUserIds)
            assertFalse(viewModel.uiState.value.isPrivate)
            assertEquals(
                "라멘팬",
                viewModel.uiState.value.profile
                    ?.nickname,
            )
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertTrue(community.requestedOffsets.isEmpty())
        }

    @Test
    fun `failed next page retries the same offset`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = (1..23).map(::review)
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            community.failNextPage = true
            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertEquals(20, viewModel.uiState.value.reviews.size)

            viewModel.dispatch(OtherReviewsIntent.Retry)
            runCurrent()
            assertEquals(listOf(0L, 20L, 20L), community.requestedOffsets)
            assertEquals(23, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `blocking replaces profile reviews and unblocking restores them`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = listOf(review(1))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
            runCurrent()
            assertTrue(viewModel.uiState.value.isBlocked)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertEquals(setOf("author"), community.blockedUserIds)

            viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
            runCurrent()
            assertFalse(viewModel.uiState.value.isBlocked)
            assertEquals(1, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `failed block leaves reviews visible`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = listOf(review(1))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            community.failNextBlock = true
            viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
            runCurrent()
            assertFalse(viewModel.uiState.value.isBlocked)
            assertEquals(1, viewModel.uiState.value.reviews.size)
        }

    private fun review(number: Int) =
        Review(
            id = "review-$number",
            shopId = "shop-$number",
            body = "맛있는 라멘이에요",
            createdAt = "2026-09-30T12:00:00Z",
            author = ReviewAuthor("author", "라멘팬"),
        )
}
