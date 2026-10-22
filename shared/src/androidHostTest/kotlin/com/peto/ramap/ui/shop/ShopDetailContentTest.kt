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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.shop.component.ShopDetailContent
import com.peto.ramap.ui.shop.menu.contract.ShopMenuLoadKey
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailExternalLinkActions
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopDetailShopActions
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewLoadKey
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.contract.ReviewsLoadKey
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
class ShopDetailContentTest {
    private var shopId by mutableStateOf("shop")
    private var overview by mutableStateOf(successfulOverview())
    private var menu by mutableStateOf(ShopMenuUiState(shopId = "shop", hasLoadFailure = true))
    private var reviews by mutableStateOf(ReviewsUiState(shopId = "shop", hasReviewLoadFailure = true))
    private var overviewRetryCount = 0
    private var menuRetryCount = 0
    private var reviewRetryCount = 0
    private var reviewLoadMoreCount = 0

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `최초 리뷰가 끝날 때까지 내용을 가리고 완료 후 메뉴 오류와 영역 재시도를 표시한다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", loadState = LoadState.loading(ReviewsLoadKey.Reviews))
            setContent { content() }
            onNodeWithTag("shop_detail_initial_skeleton").assertExists()
            onNodeWithText("테스트 매장").assertDoesNotExist()
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertDoesNotExist()
            runOnIdle { reviews = reviews.copy(loadState = LoadState(), hasReviewLoadFailure = true) }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithText("테스트 매장").assertExists()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_menu")
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertExists()
            onNodeWithText("다시 시도").performScrollTo().performClick()
            runOnIdle {
                assertEquals(1, menuRetryCount)
                assertEquals(0, overviewRetryCount)
                assertEquals(0, reviewRetryCount)
                menu = menu.copy(hasLoadFailure = false, loadState = LoadState.loading(ShopMenuLoadKey.Menu))
            }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_overview")
            onNodeWithText("테스트 매장").assertExists()
        }

    @Test
    fun `등록된 메뉴가 없으면 오류 대신 빈 메뉴 안내를 표시한다`() =
        runComposeUiTest {
            menu = ShopMenuUiState(shopId = "shop", menu = ShopMenu(sections = emptyList(), updatedAt = ""))
            setContent { content() }

            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_menu")
            onNodeWithText("등록된 메뉴가 없어요").assertExists()
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertDoesNotExist()
            onNodeWithText("다시 시도").assertDoesNotExist()
            onNodeWithText(" 업데이트").assertDoesNotExist()
        }

    @Test
    fun `오버뷰 실패는 전체 오류만 표시하고 재시도는 오버뷰에 전달한다`() =
        runComposeUiTest {
            overview = ShopOverviewState(overviewState = ShopOverviewUiState.Error("shop", ramenShopFixture(id = "shop", name = "테스트 매장")), hasLoadFailure = true)
            setContent { content() }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithText("매장 정보를 불러오지 못했어요").assertExists()
            onNodeWithText("테스트 매장").assertDoesNotExist()
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertDoesNotExist()
            onNodeWithText("리뷰를 불러오지 못했어요").assertDoesNotExist()
            onNodeWithText("다시 시도").performScrollTo().performClick()
            runOnIdle {
                assertEquals(1, overviewRetryCount)
                assertEquals(0, menuRetryCount)
                assertEquals(0, reviewRetryCount)
                overview = ShopOverviewState(overviewState = ShopOverviewUiState.Loading("shop", null), loadState = LoadState.loading(ShopOverviewLoadKey.Overview))
            }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertDoesNotExist()
            runOnIdle { overview = successfulOverview() }
            onNodeWithText("테스트 매장").assertExists()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_menu")
            onNodeWithText("메뉴 정보를 불러오지 못했어요").assertExists()
        }

    @Test
    fun `리뷰 탭 최초 실패와 재시도 중에도 오버뷰를 유지한다`() =
        runComposeUiTest {
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithText("테스트 매장").assertExists()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review_error")
            onNodeWithText("리뷰를 불러오지 못했어요").assertExists()
            onNodeWithText("다시 시도").performScrollTo().performClick()
            runOnIdle {
                assertEquals(1, reviewRetryCount)
                assertEquals(0, overviewRetryCount)
                assertEquals(0, menuRetryCount)
                reviews = reviews.copy(hasReviewLoadFailure = false, loadState = LoadState.loading(ReviewsLoadKey.Reviews))
            }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_overview")
            onNodeWithText("테스트 매장").assertExists()
        }

    @Test
    fun `다른 매장으로 전환하면 이전 성공 화면 대신 전체 스켈레톤을 표시한다`() =
        runComposeUiTest {
            setContent { content() }
            onNodeWithText("테스트 매장").assertExists()
            runOnIdle { shopId = "next" }
            onNodeWithTag("shop_detail_initial_skeleton").assertExists()
            onNodeWithText("테스트 매장").assertDoesNotExist()
        }

    @Test
    fun `닫은 바텀시트를 같은 매장으로 다시 열면 최초 완료 상태를 초기화한다`() =
        runComposeUiTest {
            var visible by mutableStateOf(true)
            setContent { if (visible) content() }
            onNodeWithText("테스트 매장").assertExists()
            runOnIdle { visible = false }
            onNodeWithText("테스트 매장").assertDoesNotExist()
            runOnIdle {
                reviews = ReviewsUiState(shopId = "shop", loadState = LoadState.loading(ReviewsLoadKey.Reviews))
                visible = true
            }
            onNodeWithTag("shop_detail_initial_skeleton").assertExists()
            onNodeWithText("테스트 매장").assertDoesNotExist()
        }

    @Test
    fun `리뷰 추가 로딩 중에는 오버뷰와 기존 리뷰를 유지한다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", hasLoaded = true, page = ShopReviewsPage(listOf(Review("review", "shop", "기존 리뷰 내용", "2026-10-14")), 2))
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review")
            onNodeWithText("기존 리뷰 내용").assertExists()
            runOnIdle { reviews = reviews.copy(loadState = LoadState.loading(ReviewsLoadKey.ReviewsPage)) }
            onNodeWithTag("shop_detail_initial_skeleton").assertDoesNotExist()
            onNodeWithText("기존 리뷰 내용").assertExists()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_overview")
            onNodeWithText("테스트 매장").assertExists()
        }

    @Test
    fun `멀리 떨어진 리뷰는 구성하지 않고 끝으로 바로 이동해도 다음 페이지를 한 번 요청한다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", hasLoaded = true, page = reviewPage(60, 100))
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithText("리뷰 내용 59").assertDoesNotExist()
            runOnIdle { assertEquals(0, reviewLoadMoreCount) }

            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-59")
            onNodeWithText("리뷰 내용 59").assertExists()
            onNodeWithText("리뷰 내용 0").assertDoesNotExist()
            runOnIdle {
                assertEquals(1, reviewLoadMoreCount)
                reviews = reviews.copy(currentProfileIsPublic = true)
            }
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }
        }

    @Test
    fun `추가 로딩 실패 후 자동 반복하지 않고 끝 근처를 벗어났다가 돌아오면 다시 요청한다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", hasLoaded = true, page = reviewPage(20, 40))
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-19")
            runOnIdle {
                assertEquals(1, reviewLoadMoreCount)
                reviews = reviews.copy(loadState = LoadState.loading(ReviewsLoadKey.ReviewsPage))
            }
            runOnIdle {
                assertEquals(1, reviewLoadMoreCount)
                reviews = reviews.copy(loadState = LoadState(), hasPageLoadFailure = true)
            }
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }

            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-0")
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-19")
            runOnIdle { assertEquals(2, reviewLoadMoreCount) }
        }

    @Test
    fun `작은 페이지를 붙인 뒤에도 끝 근처이면 이어서 요청하고 마지막 페이지에서는 중단한다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", hasLoaded = true, page = reviewPage(1, 4))
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-0")
            runOnIdle {
                assertEquals(1, reviewLoadMoreCount)
                reviews = reviews.copy(page = reviewPage(2, 4))
            }
            runOnIdle {
                assertEquals(2, reviewLoadMoreCount)
                reviews = reviews.copy(page = reviewPage(4, 4))
            }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-3")
            runOnIdle { assertEquals(2, reviewLoadMoreCount) }
        }

    @Test
    fun `메뉴 탭에서는 리뷰의 다음 페이지를 요청하지 않는다`() =
        runComposeUiTest {
            reviews = ReviewsUiState(shopId = "shop", hasLoaded = true, page = reviewPage(20, 40))
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-19")
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_tabs")
            onNodeWithText("메뉴판").performClick()
            runOnIdle { reviews = reviews.copy(page = reviewPage(21, 40)) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_detail_menu")
            runOnIdle { assertEquals(1, reviewLoadMoreCount) }
        }

    @Test
    fun `펼친 리뷰를 화면 밖으로 스크롤했다 돌아와도 펼침 상태를 유지한다`() =
        runComposeUiTest {
            val page = reviewPage(40, 40)
            reviews =
                ReviewsUiState(
                    shopId = "shop",
                    hasLoaded = true,
                    page =
                        page.copy(
                            reviews =
                                page.reviews.mapIndexed { index, review ->
                                    if (index == 0) review.copy(body = List(12) { "긴 리뷰의 본문입니다." }.joinToString("\n")) else review
                                },
                        ),
                )
            setContent { content(showReviewsOnOpen = true) }
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-0")
            onNodeWithText("더보기").performClick()
            onNodeWithText("접기").assertExists()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-39")
            onNodeWithText("접기").assertDoesNotExist()
            onNodeWithTag("shop_detail_list").performScrollToKey("shop_review:review-0")
            onNodeWithText("접기").assertExists()
        }

    @Composable
    private fun content(showReviewsOnOpen: Boolean = false) {
        val dispatcher = remember { NavigationEventDispatcher() }
        DisposableEffect(dispatcher) { onDispose { dispatcher.dispose() } }
        val owner =
            remember(dispatcher) {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            }
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            RamapTheme {
                ShopDetailContent(
                    uiState =
                        ShopDetailContentUiState(
                            shopId = shopId,
                            displayState = rememberShopDetailDisplayState(shopId, overview, menu, reviews),
                            sheetState = overview.overviewState,
                            headerState = HeaderUiState(isBookmarked = false),
                            isLoggedIn = false,
                            menuState = menu,
                            reviewState = reviews,
                            overviewHasLoadFailure = overview.hasLoadFailure,
                        ),
                    config = ShopDetailSheetConfig(isBackEnabled = true, maxHeight = 800.dp, showReviewsOnOpen = showReviewsOnOpen),
                    actions =
                        ShopDetailActions(
                            onDismissRequest = {},
                            onRetry = { overviewRetryCount++ },
                            onMenuRetry = { menuRetryCount++ },
                            shopActions = ShopDetailShopActions({}, {}, {}, {}, { _, _ -> }, {}, {}, { _, _ -> }),
                            reviewActions =
                                ReviewActions(
                                    onWriteReviewClick = {},
                                    onReviewLike = {},
                                    onRetry = { reviewRetryCount++ },
                                    onOpenProfile = {},
                                    onReviewEdit = {},
                                    onReviewDelete = {},
                                    onReviewReport = {},
                                    onLoadMoreReviews = { reviewLoadMoreCount++ },
                                ),
                            externalLinkActions = ShopDetailExternalLinkActions(onExternalLinkClick = {}),
                        ),
                )
            }
        }
    }

    private fun successfulOverview() = ShopOverviewState(overviewState = ShopOverviewUiState.Content(ShopOverView(ramenShopFixture(id = "shop", name = "테스트 매장"), 0L, null, null, null)))

    private fun reviewPage(
        count: Int,
        totalCount: Int,
    ) = ShopReviewsPage(
        reviews = List(count) { index -> Review("review-$index", "shop", "리뷰 내용 $index", "2026-10-15") },
        totalCount = totalCount,
    )
}
