package com.peto.ramap.ui.review.other

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(userId = null), FakeFollowRepository())
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            assertEquals(community.reviews, viewModel.uiState.value.reviews)
            assertEquals(23, viewModel.uiState.value.reviews.size)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `duplicate reviews across pages retain the original shop and consume the page offset`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val firstPage = (1..20).map(::review)
            community.reviews = firstPage + firstPage.first().copy(shop = ramenShopFixture(id = "shop-1"))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()

            assertEquals(firstPage, viewModel.uiState.value.reviews)
            assertEquals(21L, viewModel.uiState.value.reviewsOffset)
            assertEquals(listOf(0L, 20L), community.requestedOffsets)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `saved shops tab loads public shops in pages`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.savedShops = (1..23).map { ramenShopFixture(id = "shop-$it") }
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.SelectTab(OtherReviewsTab.SavedShops))
            runCurrent()

            assertEquals(OtherReviewsTab.SavedShops, viewModel.uiState.value.selectedTab)
            assertEquals(20, viewModel.uiState.value.savedShops.size)
            assertTrue(viewModel.uiState.value.savedShopsHasMore)

            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()

            assertEquals(listOf(0L, 20L), community.requestedSavedShopOffsets)
            assertEquals(23, viewModel.uiState.value.savedShops.size)
            assertFalse(viewModel.uiState.value.savedShopsHasMore)
        }

    @Test
    fun `guest saved shops tab requests login without fetching shops`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(userId = null), FakeFollowRepository())
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.SelectTab(OtherReviewsTab.SavedShops))
                assertEquals(OtherReviewsEffect.LoginRequired, awaitItem())
            }

            assertEquals(OtherReviewsTab.Reviews, viewModel.uiState.value.selectedTab)
            assertTrue(community.requestedSavedShopOffsets.isEmpty())
        }

    @Test
    fun `unavailable profile does not request reviews`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.profile = null
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            assertTrue(community.requestedSavedShopOffsets.isEmpty())
        }

    @Test
    fun `blocked profile keeps identity but hides reviews`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.blockedUserIds += "author"
            community.reviews = listOf(review(1))
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
            val viewModel = OtherReviewsViewModel(community, FakeProfileRepository(), FakeFollowRepository())
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
        ProfileReview(
            review =
                Review(
                    id = "review-$number",
                    shopId = "shop-$number",
                    body = "맛있는 라멘이에요",
                    createdAt = "2026-09-30T12:00:00Z",
                    author = ReviewAuthor("author", "라멘팬"),
                ),
            shop = if (number % 2 == 0) ramenShopFixture(id = "shop-$number") else null,
        )
}
