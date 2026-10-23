package com.peto.ramap.ui.shop.review

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.fake.FakeCommunityRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {
    @Test
    fun `생성 직후 매장을 선택해도 첫 조회에 로그인 사용자를 반영하고 중복 조회하지 않는다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val profile = FakeProfileRepository("owner", isProfilePublic = false)
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            var viewModel: ReviewsViewModel? = null
            val requestedViewers = mutableListOf<String?>()
            val requestedLoginStates = mutableListOf<Boolean>()
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        val state = requireNotNull(viewModel).uiState.value
                        requestedViewers += state.currentUserId
                        requestedLoginStates += state.isLoggedIn
                        return fake.fetchShopReviewsPage(shopId, offset)
                    }
                }
            viewModel = viewModel(repository, profile, login = login)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(listOf<String?>("owner"), requestedViewers.toList())
            assertEquals(listOf(true), requestedLoginStates)
            assertEquals(listOf(0L), fake.pageOffsets)
            assertEquals(false, viewModel.uiState.value.currentProfileIsPublic)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `비로그인 상태로 상세를 열면 사용자 정보 없이 리뷰를 조회한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = listOf(review("shop"))
            val viewModel = viewModel(fake)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(null, viewModel.uiState.value.currentUserId)
            assertEquals(null, viewModel.uiState.value.currentProfileIsPublic)
            assertFalse(viewModel.uiState.value.isLoggedIn)
            assertEquals(fake.reviews, viewModel.uiState.value.reviews)
            assertEquals(listOf(0L), fake.pageOffsets)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `리뷰 최초 실패와 재시도를 독립 상태로 관리한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            var failed = true
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> = if (failed) RamapResult.Error(RamapError.Http(500)) else RamapResult.Success(ShopReviewsPage(listOf(review(shopId)), 1))
                }
            val viewModel = viewModel(repository)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewLoadFailure)
            failed = false
            viewModel.dispatch(ReviewsIntent.OnRetry)
            runCurrent()
            assertEquals(1, viewModel.uiState.value.reviewCount)
            assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
        }

    @Test
    fun `같은 매장의 중복 선택은 재조회하지 않고 페이징과 새로고침에서 펼친 범위를 유지한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = (0 until 45).map { review("shop", "review-$it") }
            val viewModel = viewModel(fake)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(listOf(0L), fake.pageOffsets)
            viewModel.dispatch(ReviewsIntent.OnLoadMore)
            runCurrent()
            assertEquals(40, viewModel.uiState.value.reviews.size)
            viewModel.dispatch(ReviewsIntent.OnRetry)
            runCurrent()
            assertEquals(listOf(0L, 20L, 0L, 20L), fake.pageOffsets)
            assertEquals(40, viewModel.uiState.value.reviews.size)
            assertTrue(viewModel.uiState.value.hasMoreReviews)
        }

    @Test
    fun `새로고침 실패는 기존 리뷰를 보존하고 추가 페이지 실패로 목록을 지우지 않는다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = (0 until 30).map { review("shop", "$it") }
            var failed = false
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> = if (failed) RamapResult.Error(RamapError.Http(500)) else fake.fetchShopReviewsPage(shopId, offset)
                }
            val viewModel = viewModel(repository)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            val rows = viewModel.uiState.value.reviews
            failed = true
            viewModel.dispatch(ReviewsIntent.OnLoadMore)
            runCurrent()
            assertEquals(rows, viewModel.uiState.value.reviews)
            assertTrue(viewModel.uiState.value.hasPageLoadFailure)
            assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
            viewModel.dispatch(ReviewsIntent.OnRetry)
            runCurrent()
            assertEquals(rows, viewModel.uiState.value.reviews)
            assertTrue(viewModel.uiState.value.hasReviewLoadFailure)
        }

    @Test
    fun `매장 변경 후 늦게 도착한 리뷰 성공 응답은 새 목록을 덮지 않는다`() = lateShopResponseScenario(fails = false)

    @Test
    fun `매장 변경 후 늦게 도착한 리뷰 실패 응답은 새 목록에 오류를 표시하지 않는다`() = lateShopResponseScenario(fails = true)

    @Test
    fun `차단 접근 변경은 해당 사용자의 리뷰를 즉시 숨기고 독립 재조회한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = listOf(review("shop"))
            val changes = FakeReviewChangeStore()
            val viewModel = viewModel(fake, profile = FakeProfileRepository("owner"), changes = changes, login = FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            fake.reviews = emptyList()
            changes.publish(ReviewChange.AccessChanged("author"))
            runCurrent()
            assertEquals(emptyList(), viewModel.uiState.value.reviews)
            assertEquals(listOf(0L, 0L), fake.pageOffsets)
        }

    @Test
    fun `로그인된 상세를 열어도 변경 저장소에 같은 사용자 ID가 반영될 때까지 조회하지 않는다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = listOf(review("shop"))
            val profile = FakeProfileRepository("owner")
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val changes = FakeReviewChangeStore().apply { reset(null) }
            val viewModel = viewModel(fake, profile, changes, login)
            viewModel.dispatch(ReviewsIntent.OnShopSelected("shop"))
            runCurrent()
            viewModel.dispatch(ReviewsIntent.OnRetry)
            changes.reset("different-user")
            runCurrent()
            assertEquals(emptyList(), fake.pageOffsets)
            assertFalse(viewModel.uiState.value.hasLoaded)
            changes.reset("owner")
            runCurrent()
            assertEquals(listOf(0L), fake.pageOffsets)
            assertEquals("owner", viewModel.uiState.value.currentUserId)
            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertEquals(1, viewModel.uiState.value.reviewCount)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
        }

    private fun lateShopResponseScenario(fails: Boolean) =
        coroutinesTest {
            val fixture = ReviewsViewModelFixture()
            val response = CompletableDeferred<RamapResult<ShopReviewsPage>>()
            val oldResult = if (fails) RamapResult.Error(RamapError.Http(500)) else RamapResult.Success(ShopReviewsPage(listOf(review("old")), 1))
            var returned = false
            fixture.fetchPage = { shopId, _ ->
                if (shopId == "old") {
                    withContext(NonCancellable) { response.await() }.also { returned = true }
                } else {
                    RamapResult.Success(ShopReviewsPage(listOf(review(shopId)), 1))
                }
            }
            val viewModel = fixture.viewModel
            try {
                viewModel.dispatch(ReviewsIntent.OnShopSelected("old"))
                runCurrent()
                viewModel.dispatch(ReviewsIntent.OnShopSelected("new"))
                runCurrent()
                response.complete(oldResult)
                runCurrent()
                assertTrue(returned)
                assertEquals(listOf(review("new")), viewModel.uiState.value.reviews)
                assertTrue(viewModel.uiState.value.hasLoaded)
                assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
                assertFalse(viewModel.uiState.value.isLoading)
            } finally {
                response.complete(oldResult)
                fixture.store.clear()
            }
        }

    private fun viewModel(
        repository: ReviewRepository,
        profile: FakeProfileRepository = FakeProfileRepository(null),
        changes: FakeReviewChangeStore = FakeReviewChangeStore().apply { reset(profile.sessionUserId.value) },
        login: FakeLoginRepository = FakeLoginRepository(),
    ): ReviewsViewModel {
        val reducer = ReviewChangeReducer()
        return ReviewsViewModel(repository, FakeCommunityRepository(), profile, login, changes, reducer, FeatchShopReviewsUseCase(repository, changes, reducer, ShopReviewCache(changes, reducer)))
    }

    private fun review(
        shopId: String,
        id: String = "review",
    ) = Review(id, shopId, "맛있는 라멘", "2026-10-14", author = ReviewAuthor("author", "작성자"))
}
