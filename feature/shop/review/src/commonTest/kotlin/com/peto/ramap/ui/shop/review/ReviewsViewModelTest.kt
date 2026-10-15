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
import com.peto.ramap.ui.shop.review.contract.ShopReviewIntent
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
class ShopReviewViewModelTest {
    @Test
    fun `생성 직후 매장을 선택해도 첫 조회에 로그인 사용자를 반영하고 중복 조회하지 않는다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val profile = FakeProfileRepository("owner", isProfilePublic = false)
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            var viewModel: ShopReviewViewModel? = null
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
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(listOf<String?>("owner"), requestedViewers.toList())
            assertEquals(listOf(true), requestedLoginStates)
            assertEquals(listOf(0L), fake.pageOffsets)
            assertEquals(false, viewModel.uiState.value.currentProfileIsPublic)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `관찰 시작 전에 로그아웃해도 최신 세션과 변경 저장소 수명으로 한 번만 조회한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val profile = FakeProfileRepository("old-user")
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val changes = FakeReviewChangeStore()
            val viewModel = viewModel(fake, profile, changes, login)
            profile.sessionUserIds.value = null
            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            changes.reset(null)
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(null, viewModel.uiState.value.currentUserId)
            assertEquals(null, viewModel.uiState.value.currentProfileIsPublic)
            assertFalse(viewModel.uiState.value.isLoggedIn)
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
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewLoadFailure)
            failed = false
            viewModel.dispatch(ShopReviewIntent.OnRetry)
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
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            assertEquals(listOf(0L), fake.pageOffsets)
            viewModel.dispatch(ShopReviewIntent.OnLoadMore)
            runCurrent()
            assertEquals(40, viewModel.uiState.value.reviews.size)
            viewModel.dispatch(ShopReviewIntent.OnRetry)
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
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            val rows = viewModel.uiState.value.reviews
            failed = true
            viewModel.dispatch(ShopReviewIntent.OnLoadMore)
            runCurrent()
            assertEquals(rows, viewModel.uiState.value.reviews)
            assertTrue(viewModel.uiState.value.hasPageLoadFailure)
            assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
            viewModel.dispatch(ShopReviewIntent.OnRetry)
            runCurrent()
            assertEquals(rows, viewModel.uiState.value.reviews)
            assertTrue(viewModel.uiState.value.hasReviewLoadFailure)
        }

    @Test
    fun `매장 변경과 닫기 후 취소를 무시한 리뷰 응답은 반영하지 않는다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val old = CompletableDeferred<ShopReviewsPage>()
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> =
                        if (shopId == "old") {
                            withContext(NonCancellable) { RamapResult.Success(old.await()) }
                        } else {
                            RamapResult.Success(ShopReviewsPage(listOf(review(shopId)), 1))
                        }
                }
            val viewModel = viewModel(repository)
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("old"))
            runCurrent()
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("new"))
            runCurrent()
            old.complete(ShopReviewsPage(listOf(review("old")), 1))
            runCurrent()
            assertEquals(
                "new",
                viewModel.uiState.value.reviews
                    .single()
                    .shopId,
            )
            viewModel.dispatch(ShopReviewIntent.OnDismissed)
            viewModel.dispatch(ShopReviewIntent.OnRetry)
            runCurrent()
            assertEquals(null, viewModel.uiState.value.shopId)
            assertEquals(emptyList(), viewModel.uiState.value.reviews)
        }

    @Test
    fun `로그인 사용자가 바뀌면 이전 리뷰 범위와 일시 공개 상태를 제거한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val profile = FakeProfileRepository(null)
            profile.sessionUserIds.value = "old-user"
            val pending = CompletableDeferred<ShopReviewsPage>()
            var calls = 0
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        calls++
                        return if (calls == 1) {
                            withContext(NonCancellable) { RamapResult.Success(pending.await()) }
                        } else {
                            RamapResult.Success(ShopReviewsPage(emptyList(), 0))
                        }
                    }
                }
            val viewModel = viewModel(repository, profile)
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            profile.sessionUserIds.value = "new-user"
            runCurrent()
            pending.complete(ShopReviewsPage(listOf(review("shop").copy(isPublic = false)), 1))
            runCurrent()
            assertEquals("new-user", viewModel.uiState.value.currentUserId)
            assertEquals(emptyList(), viewModel.uiState.value.reviews)
            assertEquals(emptyMap(), viewModel.uiState.value.revealedBlockedReviews)
        }

    @Test
    fun `차단 접근 변경은 해당 사용자의 리뷰를 즉시 숨기고 독립 재조회한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            fake.reviews = listOf(review("shop"))
            val changes = FakeReviewChangeStore()
            val viewModel = viewModel(fake, changes = changes)
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            fake.reviews = emptyList()
            changes.publish(ReviewChange.AccessChanged("author"))
            runCurrent()
            assertEquals(emptyList(), viewModel.uiState.value.reviews)
            assertEquals(listOf(0L, 0L), fake.pageOffsets)
        }

    @Test
    fun `사용자 관찰자가 먼저 조회한 뒤 변경 저장소 수명이 바뀌어도 리뷰를 다시 완료한다`() =
        coroutinesTest {
            val fake = FakeShopReviewRepository()
            val profile = FakeProfileRepository(null)
            val changes = FakeReviewChangeStore()
            val pending = CompletableDeferred<ShopReviewsPage>()
            var calls = 0
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        calls++
                        return when (calls) {
                            2 -> RamapResult.Success(pending.await())
                            else -> RamapResult.Success(ShopReviewsPage(listOf(review(shopId)), 1))
                        }
                    }
                }
            val viewModel = viewModel(repository, profile, changes)
            viewModel.dispatch(ShopReviewIntent.OnShopSelected("shop"))
            runCurrent()
            profile.sessionUserIds.value = "new-user"
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            changes.reset("new-user")
            runCurrent()
            assertEquals(3, calls)
            assertEquals("new-user", viewModel.uiState.value.currentUserId)
            assertEquals(1, viewModel.uiState.value.reviewCount)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.hasReviewLoadFailure)
        }

    private fun viewModel(
        repository: ReviewRepository,
        profile: FakeProfileRepository = FakeProfileRepository(null),
        changes: FakeReviewChangeStore = FakeReviewChangeStore(),
        login: FakeLoginRepository = FakeLoginRepository(),
    ): ShopReviewViewModel {
        val reducer = ReviewChangeReducer()
        return ShopReviewViewModel(repository, FakeCommunityRepository(), profile, login, changes, reducer, FeatchShopReviewsUseCase(repository, changes, reducer, ShopReviewCache(changes, reducer)))
    }

    private fun review(
        shopId: String,
        id: String = "review",
    ) = Review(id, shopId, "맛있는 라멘", "2026-10-14", author = ReviewAuthor("author", "작성자"))
}
