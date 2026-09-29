package com.peto.ramap.ui.profile.review

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.ui.profile.review.contract.ProfileReviewIntent
import com.peto.ramap.ui.profile.review.contract.ProfileReviewSideEffect
import com.peto.ramap.ui.profile.review.contract.ProfileReviewTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileReviewViewModelTest {
    @Test
    fun `like rejects own review and requires login for another public review`() =
        coroutinesTest {
            val reviews = FakeReviewRepository()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val profiles = FakeProfileRepository()
            val viewModel = ProfileReviewViewModel(reviews, FakeCommunityRepository(), login, profiles)
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("me")))
            runCurrent()
            viewModel.dispatch(ProfileReviewIntent.ToggleLike(review("own")))
            runCurrent()
            assertTrue(reviews.likedRequests.isEmpty())

            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            profiles.sessionUserIds.value = null
            runCurrent()
            viewModel.sideEffect.test {
                viewModel.dispatch(ProfileReviewIntent.ToggleLike(review("other", "other")))
                assertEquals(ProfileReviewSideEffect.LoginRequired, awaitItem())
            }
            assertTrue(reviews.likedRequests.isEmpty())
        }

    @Test
    fun `own review deletion confirms then removes review`() =
        coroutinesTest {
            val reviews = FakeReviewRepository(profileReviews = listOf(review("own")))
            val viewModel =
                ProfileReviewViewModel(
                    reviews,
                    FakeCommunityRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                    FakeProfileRepository(),
                )
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("me")))
            runCurrent()
            viewModel.dispatch(ProfileReviewIntent.ConfirmDelete(review("own")))
            runCurrent()
            assertTrue(reviews.deletedReviewIds.isEmpty())
            viewModel.dispatch(ProfileReviewIntent.DeleteReview)
            runCurrent()
            assertEquals(listOf("own"), reviews.deletedReviewIds)
            assertEquals(null, viewModel.uiState.value.deleteTarget)
        }

    @Test
    fun `opening another profile clears previous reviews and loads new target`() =
        coroutinesTest {
            val other = PublicProfile("other", "다른사용자")
            val reviews = FakeReviewRepository(profileReviews = listOf(review("first")))
            val viewModel =
                ProfileReviewViewModel(
                    reviews,
                    FakeCommunityRepository(publicProfiles = mapOf(other.userId to other)),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                    FakeProfileRepository(),
                )
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("me")))
            runCurrent()
            assertEquals(
                listOf("first"),
                viewModel.uiState.value.reviews
                    .map { it.id },
            )

            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget(other.userId)))
            runCurrent()

            assertEquals(
                listOf<Pair<String?, Long>>("me" to 0L, other.userId to 0L),
                reviews.profileRequests,
            )
            assertEquals(other, viewModel.uiState.value.profile)
        }

    @Test
    fun `self profile uses its user id and includes pending reviews`() =
        coroutinesTest {
            val reviews = FakeReviewRepository(profileReviews = listOf(review("pending")))
            val viewModel = ProfileReviewViewModel(reviews, FakeCommunityRepository(), FakeLoginRepository(LoginSessionState.AUTHENTICATED), FakeProfileRepository())
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("me")))
            runCurrent()
            assertEquals(listOf<Pair<String?, Long>>("me" to 0L), reviews.profileRequests)
            assertEquals(1, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `guest report requests login while signed in report has success and failure states`() =
        coroutinesTest {
            val review = review("report", authorId = "author")
            val login = FakeLoginRepository()
            val profiles = FakeProfileRepository(null)
            val community = FakeCommunityRepository()
            val viewModel = ProfileReviewViewModel(FakeReviewRepository(), community, login, profiles)
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("author")))
            runCurrent()
            viewModel.sideEffect.test {
                viewModel.dispatch(ProfileReviewIntent.ReportReview(review))
                assertEquals(ProfileReviewSideEffect.LoginRequired, awaitItem())
            }
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            profiles.sessionUserIds.value = "me"
            runCurrent()
            viewModel.dispatch(ProfileReviewIntent.ReportReview(review))
            viewModel.dispatch(ProfileReviewIntent.SendReport(ReportReason.SPAM, "spam"))
            runCurrent()
            assertTrue(viewModel.uiState.value.actionSucceeded)
            assertEquals(listOf("report"), community.reportedReviews)

            community.reportError = RamapError.Network(IllegalStateException("offline"))
            viewModel.dispatch(ProfileReviewIntent.ReportReview(review))
            viewModel.dispatch(ProfileReviewIntent.SendReport(ReportReason.SPAM, ""))
            runCurrent()
            assertTrue(viewModel.uiState.value.actionFailed)
        }

    @Test
    fun `block and unblock refresh the feed and remove blocked content`() =
        coroutinesTest {
            val profile = PublicProfile("other", "다른사용자")
            val community = FakeCommunityRepository(publicProfiles = mapOf(profile.userId to profile))
            val viewModel = ProfileReviewViewModel(FakeReviewRepository(profileReviews = listOf(review("one", authorId = profile.userId))), community, FakeLoginRepository(LoginSessionState.AUTHENTICATED), FakeProfileRepository())
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget(profile.userId)))
            runCurrent()
            viewModel.dispatch(ProfileReviewIntent.ConfirmBlock(profile))
            viewModel.dispatch(ProfileReviewIntent.ToggleBlock)
            runCurrent()
            assertTrue(community.blocked.contains(profile.userId))
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            viewModel.dispatch(ProfileReviewIntent.ConfirmBlock(profile))
            viewModel.dispatch(ProfileReviewIntent.ToggleBlock)
            runCurrent()
            assertFalse(community.blocked.contains(profile.userId))
        }

    @Test
    fun `direct account switch clears reviews dialogs and ignores late first account response`() =
        coroutinesTest {
            val profiles = FakeProfileRepository("account-a")
            val pending = CompletableDeferred<RamapResult<List<Review>>>()
            val reviews = FakeReviewRepository(profileReviews = listOf(review("new")))
            reviews.nextProfileResponse = pending
            val viewModel = ProfileReviewViewModel(reviews, FakeCommunityRepository(), FakeLoginRepository(LoginSessionState.AUTHENTICATED), profiles)
            viewModel.dispatch(ProfileReviewIntent.OpenTarget(ProfileReviewTarget("me")))
            runCurrent()
            viewModel.dispatch(ProfileReviewIntent.ReportReview(review("report", "other")))
            runCurrent()
            profiles.sessionUserIds.value = "account-b"
            runCurrent()
            assertEquals(null, viewModel.uiState.value.reportTargetId)
            assertEquals(
                listOf("new"),
                viewModel.uiState.value.reviews
                    .map { it.id },
            )
            pending.complete(RamapResult.Success(listOf(review("old-private"))))
            runCurrent()
            assertEquals(
                listOf("new"),
                viewModel.uiState.value.reviews
                    .map { it.id },
            )
        }

    private fun review(
        id: String,
        authorId: String = "me",
    ) = Review(id, "shop", "review", "today", author = ReviewAuthor(authorId, "작성자"))
}
