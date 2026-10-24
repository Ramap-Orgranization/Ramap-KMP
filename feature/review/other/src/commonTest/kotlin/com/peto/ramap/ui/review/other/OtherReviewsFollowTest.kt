package com.peto.ramap.ui.review.other

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowState
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
import com.peto.ramap.ui.review.other.contract.OtherReviewsUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_load_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OtherReviewsFollowTest {
    @Test
    fun `팔로우와 언팔로우 변경 알림은 기존 프로필과 여러 페이지 목록 및 선택 탭을 유지한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community =
                FakeOtherReviewsCommunity().apply {
                    reviews = (1..23).map(::review)
                    savedShops = listOf(ramenShopFixture())
                    reviewCount = 23L
                }
            val follows =
                FakeFollowRepository().apply {
                    onChanged = { userId, action -> changes.publish(ReviewChange.AccessChanged(userId, action)) }
                }
            val viewModel = createViewModel(community, changes, follows)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.SelectTab(OtherReviewsTab.SavedShops))
            runCurrent()
            val original = viewModel.uiState.value
            val states = mutableListOf<OtherReviewsUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(states) }

            for (followState in listOf(FollowState.FOLLOWING, FollowState.NONE)) {
                val pending = CompletableDeferred<RamapResult<ProfileAccess>>()
                community.profileAccessPending = pending
                viewModel.dispatch(OtherReviewsIntent.ToggleFollow)
                runCurrent()

                assertTrue(viewModel.uiState.value.following)
                assertEquals(original.profile, viewModel.uiState.value.profile)
                assertEquals(original.reviews, viewModel.uiState.value.reviews)
                assertEquals(original.savedShops, viewModel.uiState.value.savedShops)
                assertEquals(OtherReviewsTab.SavedShops, viewModel.uiState.value.selectedTab)
                assertEquals(listOf(0L, 20L), community.requestedOffsets)
                assertEquals(listOf(0L), community.requestedSavedShopOffsets)

                val followerCount = if (followState == FollowState.FOLLOWING) 1L else 0L
                val access = assertIs<ProfileAccess.Visible>(original.profileAccess)
                pending.complete(RamapResult.Success(access.copy(followState = followState, followerCount = followerCount)))
                runCurrent()

                val updated = assertIs<ProfileAccess.Visible>(viewModel.uiState.value.profileAccess)
                assertEquals(followState, updated.followState)
                assertEquals(followerCount, updated.followerCount)
                assertFalse(viewModel.uiState.value.following)
            }

            assertTrue(states.all { it.profile == original.profile && it.reviews == original.reviews && it.savedShops == original.savedShops })
            assertTrue(states.none { it.showsInitialLoading || it.loading || it.savedShopsLoading })
            assertEquals(listOf("author", "author", "author"), community.requestedProfileUserIds)
            assertEquals(original.reviewsOffset, viewModel.uiState.value.reviewsOffset)
            assertEquals(original.savedShopsOffset, viewModel.uiState.value.savedShopsOffset)
        }

    @Test
    fun `다른 화면의 팔로우 변경 알림도 기존 목록을 유지하며 접근 상태만 갱신한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community = FakeOtherReviewsCommunity().apply { reviews = listOf(review(1)) }
            val viewModel = createViewModel(community, changes)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            val original = viewModel.uiState.value
            val pending = CompletableDeferred<RamapResult<ProfileAccess>>()
            community.profileAccessPending = pending

            changes.publish(ReviewChange.AccessChanged("author", FollowAction.FOLLOW))
            runCurrent()

            assertEquals(original.profile, viewModel.uiState.value.profile)
            assertEquals(original.reviews, viewModel.uiState.value.reviews)
            assertFalse(viewModel.uiState.value.showsInitialLoading)
            val access = assertIs<ProfileAccess.Visible>(original.profileAccess)
            pending.complete(RamapResult.Success(access.copy(followState = FollowState.FOLLOWING, followerCount = 1L)))
            runCurrent()

            assertEquals(FollowState.FOLLOWING, assertIs<ProfileAccess.Visible>(viewModel.uiState.value.profileAccess).followState)
            assertEquals(listOf(0L), community.requestedOffsets)
        }

    @Test
    fun `언팔로우로 리뷰 열람 권한만 잃으면 프로필과 저장 목록은 유지한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community =
                FakeOtherReviewsCommunity().apply {
                    followState = FollowState.FOLLOWING
                    reviews = listOf(review(1))
                    savedShops = listOf(ramenShopFixture())
                }
            val follows =
                FakeFollowRepository().apply {
                    onChanged = { userId, action ->
                        community.followState = FollowState.NONE
                        community.canReadReviews = false
                        changes.publish(ReviewChange.AccessChanged(userId, action))
                    }
                }
            val viewModel = createViewModel(community, changes, follows)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            val original = viewModel.uiState.value

            viewModel.dispatch(OtherReviewsIntent.ToggleFollow)
            runCurrent()

            assertEquals(original.profile, viewModel.uiState.value.profile)
            assertTrue(viewModel.uiState.value.contentRestricted)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertEquals(0L, viewModel.uiState.value.reviewsOffset)
            assertEquals(original.savedShops, viewModel.uiState.value.savedShops)
            assertEquals(listOf(0L), community.requestedOffsets)
            assertEquals(listOf(0L), community.requestedSavedShopOffsets)
        }

    @Test
    fun `팔로우로 열람 권한을 얻으면 프로필을 유지하며 새로 허용된 목록을 불러온다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community =
                FakeOtherReviewsCommunity().apply {
                    canReadReviews = false
                    canReadSavedShops = false
                    reviews = listOf(review(1))
                    savedShops = listOf(ramenShopFixture())
                }
            val follows =
                FakeFollowRepository().apply {
                    onChanged = { userId, action ->
                        community.followState = FollowState.FOLLOWING
                        community.canReadReviews = true
                        community.canReadSavedShops = true
                        changes.publish(ReviewChange.AccessChanged(userId, action))
                    }
                }
            val viewModel = createViewModel(community, changes, follows)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            val originalProfile = assertNotNull(viewModel.uiState.value.profile)
            assertTrue(community.requestedOffsets.isEmpty())
            assertTrue(community.requestedSavedShopOffsets.isEmpty())

            viewModel.dispatch(OtherReviewsIntent.ToggleFollow)
            runCurrent()

            assertEquals(originalProfile, viewModel.uiState.value.profile)
            assertFalse(viewModel.uiState.value.contentRestricted)
            assertEquals(community.reviews, viewModel.uiState.value.reviews)
            assertEquals(community.savedShops, viewModel.uiState.value.savedShops)
            assertEquals(listOf(0L), community.requestedOffsets)
            assertEquals(listOf(0L), community.requestedSavedShopOffsets)
        }

    @Test
    fun `팔로우 후 접근 상태 조회가 실패하면 기존 화면을 유지하고 실패를 안내한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community = FakeOtherReviewsCommunity().apply { reviews = listOf(review(1)) }
            val follows =
                FakeFollowRepository().apply {
                    onChanged = { userId, action -> changes.publish(ReviewChange.AccessChanged(userId, action)) }
                }
            val viewModel = createViewModel(community, changes, follows)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            val original = viewModel.uiState.value
            community.profileAccessPending = CompletableDeferred(RamapResult.Error(RamapError.Http(500)))

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.ToggleFollow)
                runCurrent()
                val effect = assertIs<OtherReviewsEffect.ShowToast>(awaitItem())
                assertEquals(Res.string.follow_load_failure_message, effect.data.message)
            }

            assertEquals(original.profileAccess, viewModel.uiState.value.profileAccess)
            assertEquals(original.reviews, viewModel.uiState.value.reviews)
            assertFalse(viewModel.uiState.value.following)
            assertFalse(viewModel.uiState.value.failed)
        }

    @Test
    fun `팔로우 외 접근 변경 알림은 기존 콘텐츠를 즉시 숨기고 다시 확인한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community = FakeOtherReviewsCommunity().apply { reviews = listOf(review(1)) }
            val viewModel = createViewModel(community, changes)
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<ProfileAccess>>()
            community.profileAccessPending = pending

            changes.publish(ReviewChange.AccessChanged("author"))
            runCurrent()

            assertEquals(null, viewModel.uiState.value.profile)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            pending.complete(RamapResult.Success(ProfileAccess.Unavailable))
            runCurrent()
            assertTrue(viewModel.uiState.value.showsUnavailableProfile)
        }

    private fun createViewModel(
        community: FakeOtherReviewsCommunity,
        changes: FakeReviewChangeStore,
        follows: FakeFollowRepository = FakeFollowRepository(),
    ) = OtherReviewsViewModel(
        community = community,
        profiles = FakeProfileRepository(),
        follows = follows,
        reviewRepository = FakeShopReviewRepository(),
        changes = changes,
        reviewChangeReducer = ReviewChangeReducer(),
    )

    private fun review(number: Int) =
        ProfileReview(
            review = Review("review-$number", "shop", "맛있는 라멘이에요", "2026-10-24", author = ReviewAuthor("author", "라멘팬")),
        )
}
