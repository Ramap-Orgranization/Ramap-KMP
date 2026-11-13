package com.peto.ramap.ui

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.profile.ProfileAccess
import com.peto.ramap.domain.model.profile.ProfileReviewsPage
import com.peto.ramap.domain.model.profile.PublicProfile
import com.peto.ramap.domain.model.profile.PublicSavedShopsPage
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewFeedRepository
import com.peto.ramap.domain.repository.UserProfileRepository
import com.peto.ramap.domain.usecase.RefreshReviewListUseCase
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeProfileVisitRankingRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.fake.FakeUserProfileRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.main.feed.FeedRoute
import com.peto.ramap.ui.main.feed.FeedViewModel
import com.peto.ramap.ui.review.other.OtherReviewsRoute
import com.peto.ramap.ui.review.other.OtherReviewsViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ReviewLoginScrollTest {
    private val profile = FakeProfileRepository(null)
    private val changes = FakeReviewChangeStore().apply { reset(null) }
    private val reducer = ReviewChangeReducer()
    private val refreshReviews = RefreshReviewListUseCase(reducer)
    private val toastManager = ToastManager()
    private val loginModule = module { single { LoginGuideController() } }
    private val rows =
        List(18) { index ->
            ProfileReview(
                review = Review("review-$index", "shop", "라멘 리뷰 $index", "2026-10-14", author = ReviewAuthor("author", "작성자")),
                shop = ramenShopFixture(id = "shop"),
            )
        }

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `피드에서 로그인해도 보던 리뷰의 세로 위치를 유지한다`() =
        runComposeUiTest {
            val response = CompletableDeferred<Unit>()
            val repository =
                object : ReviewFeedRepository {
                    override suspend fun fetchPage(
                        cursor: ReviewFeedCursor?,
                        sort: ReviewFeedSort,
                        filter: ReviewFeedFilter,
                    ): RamapResult<ReviewFeedPage> {
                        if (changes.journal.value.userId != null) response.await()
                        return RamapResult.Success(ReviewFeedPage(rows, null))
                    }

                    override suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?> = RamapResult.Success(null)
                }
            val viewModel = FeedViewModel(repository, FakeUserModerationRepository(), profile, FakeShopReviewRepository(), changes, reducer, refreshReviews)
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(loginModule) }) {
                    RamapTheme {
                        FeedRoute(
                            onOpenProfile = {},
                            onEditReview = { _, _ -> },
                            onLoginTypeSelected = {},
                            onFindVisitedShop = {},
                            shopDetailContent = { _, _ -> },
                            viewModel = viewModel,
                            toastManager = toastManager,
                        )
                    }
                }
            }

            onNode(hasScrollToIndexAction()).performScrollToKey("review-11")
            val review = onNodeWithText("라멘 리뷰 11")
            val top = review.getBoundsInRoot().top
            runOnIdle { signIn() }
            assertEquals(top, review.assertIsDisplayed().getBoundsInRoot().top)
            runOnIdle { response.complete(Unit) }
            assertEquals(top, review.assertIsDisplayed().getBoundsInRoot().top)
        }

    @Test
    fun `다른 사용자 프로필에서 로그인해도 보던 리뷰의 세로 위치를 유지한다`() =
        runComposeUiTest {
            val response = CompletableDeferred<Unit>()
            val access = ProfileAccess.Visible(PublicProfile("author", "작성자"), canReadSavedShops = false)
            val repository =
                object : UserProfileRepository by FakeUserProfileRepository() {
                    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = RamapResult.Success(access)

                    override suspend fun fetchUserSavedShops(
                        userId: String,
                        offset: Long,
                    ): RamapResult<PublicSavedShopsPage> = RamapResult.Success(PublicSavedShopsPage(access, emptyList()))

                    override suspend fun fetchProfileReviewsPage(
                        userId: String,
                        offset: Long,
                    ): RamapResult<ProfileReviewsPage> {
                        if (profile.sessionUserId.value != null) response.await()
                        return RamapResult.Success(ProfileReviewsPage(access, rows))
                    }
                }
            val viewModel =
                OtherReviewsViewModel(
                    userProfileRepository = repository,
                    userModerationRepository = FakeUserModerationRepository(),
                    profiles = profile,
                    follows = FakeFollowRepository(),
                    changes = changes,
                    reviewChangeReducer = reducer,
                    visitRankings = FakeProfileVisitRankingRepository(),
                    refreshReviews = refreshReviews,
                )
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(loginModule) }) {
                    RamapTheme {
                        OtherReviewsRoute(
                            userId = "author",
                            onBack = {},
                            onLoginTypeSelected = {},
                            onShowShop = {},
                            onEventNavigate = {},
                            shopDetailContent = { _, _, _, _ -> },
                            viewModel = viewModel,
                            toastManager = toastManager,
                        )
                    }
                }
            }

            onNode(hasScrollToIndexAction()).performScrollToKey("review-11")
            val review = onNodeWithText("라멘 리뷰 11")
            val top = review.getBoundsInRoot().top
            runOnIdle { signIn() }
            assertEquals(top, review.assertIsDisplayed().getBoundsInRoot().top)
            runOnIdle { response.complete(Unit) }
            assertEquals(top, review.assertIsDisplayed().getBoundsInRoot().top)
        }

    private fun signIn() {
        profile.sessionUserId.value = "viewer"
        changes.reset("viewer")
    }
}
