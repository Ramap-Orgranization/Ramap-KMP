package com.peto.ramap.ui.shop

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.archive.ArchiveShopDetailContent
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailTab
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailUiModel
import com.peto.ramap.ui.shop.component.ShopDetailBody
import com.peto.ramap.ui.shop.component.ShopDetailBottomSheet
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailDisplayState
import com.peto.ramap.ui.shop.model.ShopDetailExternalLinkActions
import com.peto.ramap.ui.shop.model.ShopDetailShopActions
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.shop.review.model.ReviewActions
import org.junit.runner.RunWith
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
class ArchiveShopDetailContentTest {
    private val shop = ramenShopFixture(id = "shop", name = "테스트 매장")
    private var reviews by mutableStateOf(ReviewsUiState(shopId = "shop", hasLoaded = true, page = reviewPage(1, 1)))
    private var reviewLoadMoreCount = 0

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `기록 탭이 먼저 나오고 매장 정보에서는 기존 메뉴와 전체 리뷰를 표시한다`() =
        runComposeUiTest {
            setContent { content() }
            onNodeWithText("나만의 방문 기록").assertExists()
            onNodeWithText("전체 리뷰 0").assertDoesNotExist()
            onNodeWithText("매장 정보").performClick()
            onNodeWithText("나만의 방문 기록").assertDoesNotExist()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_tabs")
            onNodeWithText("메뉴판").assertExists()
            onNodeWithText("리뷰").performClick()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-0")
            onNodeWithText("전체 리뷰 0").assertExists()
            onNodeWithText("내 리뷰").performClick()
            onNodeWithText("나만의 방문 기록").assertExists()
            onNodeWithText("매장 정보").performClick()
            onNodeWithText("전체 리뷰 0").assertExists()
        }

    @Test
    fun `전체 리뷰의 추가 조회는 정보 탭에서만 동작하고 기록으로 돌아오면 중단한다`() =
        runComposeUiTest {
            reviews = reviews.copy(page = reviewPage(20, 40))
            setContent { content() }
            runOnIdle { assertEquals(0, reviewLoadMoreCount) }
            onNodeWithText("매장 정보").performClick()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_tabs")
            onNodeWithText("리뷰").performClick()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-19")
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }
            onNodeWithText("내 리뷰").performClick()
            runOnIdle { reviews = reviews.copy(page = reviewPage(21, 40)) }
            onNodeWithText("나만의 방문 기록").assertExists()
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }
            onNodeWithText("매장 정보").performClick()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-20")
            runOnIdle { assertEquals(2, reviewLoadMoreCount) }
        }

    @Composable
    private fun content() {
        val dispatcher = remember { NavigationEventDispatcher() }
        DisposableEffect(dispatcher) { onDispose { dispatcher.dispose() } }
        val owner =
            remember(dispatcher) {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            }
        val reviewActions =
            ReviewActions(
                onWriteReviewClick = {},
                onReviewLike = {},
                onRetry = {},
                onOpenProfile = {},
                onReviewEdit = {},
                onReviewDelete = {},
                onReviewReport = {},
                onLoadMoreReviews = { reviewLoadMoreCount++ },
            )
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            RamapTheme {
                ShopDetailBody(
                    uiState =
                        ShopDetailContentUiState(
                            shopId = shop.id,
                            displayState = ShopDetailDisplayState.Content,
                            sheetState = ShopOverviewUiState.Content(ShopOverView(shop, 0L, null, null, null)),
                            headerState = HeaderUiState(isBookmarked = false),
                            isLoggedIn = true,
                            menuState = ShopMenuUiState(shopId = shop.id, hasLoadFailure = true),
                            reviewState = reviews,
                        ),
                    actions =
                        ShopDetailActions(
                            onDismissRequest = {},
                            onRetry = {},
                            shopActions = ShopDetailShopActions({}, {}, {}, {}, { _, _ -> }, {}, {}, { _, _ -> }),
                            reviewActions = reviewActions,
                            externalLinkActions = ShopDetailExternalLinkActions(onExternalLinkClick = {}),
                        ),
                    showDismissButton = false,
                ) { informationItems, loadMoreState, loadMoreEnabled ->
                    ArchiveShopDetailContent(
                        request =
                            ArchiveShopDetailUiModel(
                                shop = shop,
                                reviews = listOf(MyReview(Review("mine", shop.id, "나만의 방문 기록", "2026-10-27", isPublic = false), shop.name)),
                                isReviewPhotosLoading = false,
                                hasReviewPhotoError = false,
                                reviewPhotoRetryKeys = emptyMap(),
                                onRetryReviewPhotos = {},
                                onReviewPhotoDownloadFailed = {},
                                onReviewPhotoDownloadSucceeded = {},
                                deleteTargetReviewId = null,
                                isDeletingReview = false,
                                onRequestReviewDeletion = {},
                                onDismissReviewDeletion = {},
                                onConfirmReviewDeletion = {},
                                onDismiss = {},
                            ),
                        onEditReview = { _, _ -> },
                        sheetContent = { listState, tab, items ->
                            ShopDetailBottomSheet(
                                shopId = shop.id,
                                listState = listState,
                                maxHeight = 800.dp,
                                isBackEnabled = true,
                                isNavigationBarPadded = false,
                                onDismissRequest = {},
                                loadMoreState = loadMoreState,
                                loadMoreEnabled = tab == ArchiveShopDetailTab.INFORMATION && loadMoreEnabled,
                                onLoadMore = reviewActions.onLoadMoreReviews,
                                collapsedHeightFraction = 0.7f,
                                content = items,
                            )
                        },
                        shopInformationContent = informationItems,
                    )
                }
            }
        }
    }

    private fun reviewPage(
        count: Int,
        totalCount: Int,
    ) = ShopReviewsPage(
        reviews = List(count) { index -> Review("review-$index", shop.id, "전체 리뷰 $index", "2026-10-15") },
        totalCount = totalCount,
    )
}
