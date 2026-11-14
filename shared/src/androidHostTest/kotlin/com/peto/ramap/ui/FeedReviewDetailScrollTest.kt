package com.peto.ramap.ui

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewFeedRepository
import com.peto.ramap.domain.usecase.RefreshReviewListUseCase
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.main.feed.FeedRoute
import com.peto.ramap.ui.main.feed.FeedViewModel
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class FeedReviewDetailScrollTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `리뷰 상세에서 돌아오면 피드 위치를 복원하고 정렬 필터 또는 하단 탭을 바꾸면 초기화한다`() =
        runComposeUiTest {
            val shop = ramenShopFixture(id = "shop")
            val rows =
                List(18) { index ->
                    ProfileReview(
                        review = Review("review-$index", shop.id, "라멘 리뷰 $index", "2026-10-14", author = ReviewAuthor("author", "작성자")),
                        shop = shop,
                    )
                }
            val repository =
                object : ReviewFeedRepository {
                    override suspend fun fetchPage(
                        cursor: ReviewFeedCursor?,
                        sort: ReviewFeedSort,
                        filter: ReviewFeedFilter,
                    ): RamapResult<ReviewFeedPage> = RamapResult.Success(ReviewFeedPage(rows, null))

                    override suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?> = RamapResult.Success(null)
                }
            val feedModule =
                module {
                    single { LoginGuideController() }
                    viewModel {
                        val reducer = ReviewChangeReducer()
                        FeedViewModel(
                            repository,
                            FakeUserModerationRepository(),
                            FakeProfileRepository("viewer"),
                            FakeShopReviewRepository(),
                            FakeReviewChangeStore().apply { reset("viewer") },
                            reducer,
                            RefreshReviewListUseCase(reducer),
                        )
                    }
                }
            val toastManager = ToastManager()
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(feedModule) }) {
                    RamapTheme {
                        val navigation = rememberNavigationState()
                        NavigationRouter(
                            navigationState = navigation,
                            mapScreen = {},
                            archiveMapScreen = {},
                            reviewShopSearchScreen = {},
                            rankingScreen = {},
                            feedScreen = {
                                FeedRoute(
                                    onOpenProfile = {},
                                    onEditReview = { _, _ -> },
                                    onLoginTypeSelected = {},
                                    onFindVisitedShop = {},
                                    shopDetailContent = { _, _ -> },
                                    toastManager = toastManager,
                                    onOpenReviewDetail = navigation::showReviewDetail,
                                )
                            },
                            newsScreen = {},
                            myScreen = {},
                            settingsScreen = {},
                            accountSettingsScreen = {},
                            profileEditScreen = {},
                            announcementScreen = {},
                            myReviewsScreen = {},
                            otherReviewsScreen = {},
                            reviewDetailScreen = { route ->
                                Button(onClick = navigation::pop) { Text("상세 ${route.reviewId}") }
                            },
                            reviewWriteScreen = {},
                            informationScreen = {},
                            placeReportScreen = {},
                            notificationSettingsScreen = {},
                            subscribedShopsScreen = {},
                            bookmarkedShopsScreen = {},
                            importationScreen = {},
                            importationGuideScreen = {},
                            eventScreen = {},
                        )
                    }
                }
            }

            onNodeWithText("피드").performClick()
            val list = onNode(hasScrollToIndexAction())
            val review = onNodeWithText("라멘 리뷰 11")
            list.performScrollToKey("review-11")
            val alignedTop = review.getBoundsInRoot().top
            list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 24f) }
            val scrolledTop = review.getBoundsInRoot().top
            assertNotEquals(alignedTop, scrolledTop)

            review.performClick()
            onNodeWithText("상세 review-11").assertIsDisplayed().performClick()
            assertEquals(scrolledTop, review.assertIsDisplayed().getBoundsInRoot().top)

            onNodeWithText("최신순").performClick()
            onNodeWithText("인기순").performClick()
            onNodeWithText("라멘 리뷰 0").assertIsDisplayed()

            list.performScrollToKey("review-11")
            onNodeWithText("저장한 매장").performClick()
            onNodeWithText("라멘 리뷰 0").assertIsDisplayed()

            onNodeWithText("인기순").performClick()
            onNodeWithText("최신순").performClick()
            onNodeWithText("전체").performClick()
            list.performScrollToKey("review-11")
            review.assertIsDisplayed()
            onNodeWithText("지도").performClick()
            onNodeWithText("지도").assertIsSelected()
            onNodeWithText("피드").performClick()

            onNodeWithText("최신순").assertIsDisplayed()
            onNodeWithText("라멘 리뷰 0").assertIsDisplayed()
        }
}
