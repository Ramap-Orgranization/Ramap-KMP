package com.peto.ramap.ui.bookmark.list

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_success_message
import ramap.shared.generated.resources.personalization_update_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkedShopListViewModelTest {
    @Test
    fun `좋아요 매장 화면 진입시 개인화 아이디에 해당하는 매장을 로드한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(listOf(shop)),
                        ),
                )

            runCurrent()

            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
        }

    @Test
    fun `공유 북마크 상태가 비어 있으면 빈 목록을 표시한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    ramenShopRepository = ramenShopRepository,
                )

            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
            assertEquals(emptyList(), ramenShopRepository.requestedShopIdsHistory)
        }

    @Test
    fun `매장 조회에 실패하면 오류 상태를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            error = RamapError.Unknown(IllegalStateException("failure")),
                        ),
                )

            runCurrent()

            assertEquals(true, viewModel.uiState.value.showError)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
        }

    @Test
    fun `매장 조회 실패 후 재시도하면 현재 북마크 아이디로 다시 조회한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(listOf(shop)),
                    error = RamapError.Unknown(IllegalStateException("failure")),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            ramenShopRepository.error = null
            viewModel.dispatch(BookmarkedShopListIntent.OnRetry)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.showError)
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
            assertEquals(
                listOf(setOf(shop.id), setOf(shop.id)),
                ramenShopRepository.requestedShopIdsHistory,
            )
        }

    @Test
    fun `매장 재조회를 시작하면 이전 오류를 해제하고 성공 결과를 표시한다`() =
        coroutinesTest {
            val initialShop = ramenShopFixture(id = "initial-bookmarked-shop")
            val recoveredShop = ramenShopFixture(id = "recovered-bookmarked-shop")
            val recoveredResult = CompletableDeferred<RamapResult<RamenShops>>()
            var requestCount = 0
            val ramenShopRepository =
                object : RamenShopRepository by FakeRamenShopRepository() {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        requestCount += 1
                        return if (requestCount == 1) {
                            RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
                        } else {
                            recoveredResult.await()
                        }
                    }
                }
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(initialShop.id)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showError)

            personalizationRepository.updateBookmarkedShopIds(setOf(recoveredShop.id))
            runCurrent()

            assertEquals(false, viewModel.uiState.value.showError)
            assertEquals(true, viewModel.uiState.value.isOnlyLoading)

            recoveredResult.complete(RamapResult.Success(RamenShops(listOf(recoveredShop))))
            runCurrent()

            assertEquals(false, viewModel.uiState.value.showError)
            assertEquals(RamenShops(listOf(recoveredShop)), viewModel.uiState.value.shops)
        }

    @Test
    fun `공유 북마크 상태의 제거와 추가를 목록에 동기화한다`() =
        coroutinesTest {
            val initialShop = ramenShopFixture(id = "initial-bookmarked-shop", name = "기존 매장")
            val addedShop = ramenShopFixture(id = "added-bookmarked-shop", name = "추가 매장")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(initialShop.id)),
                )
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(listOf(initialShop, addedShop)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            personalizationRepository.updateBookmarkedShopIds(emptySet())
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)

            personalizationRepository.updateBookmarkedShopIds(setOf(addedShop.id))
            runCurrent()

            assertEquals(RamenShops(listOf(addedShop)), viewModel.uiState.value.shops)
            assertEquals(
                listOf(setOf(initialShop.id), setOf(addedShop.id)),
                ramenShopRepository.requestedShopIdsHistory,
            )
        }

    @Test
    fun `북마크 아이디가 연속 변경되면 이전 조회를 취소하고 최신 결과만 반영한다`() =
        coroutinesTest {
            val initialShop = ramenShopFixture(id = "initial-shop")
            val latestShop = ramenShopFixture(id = "latest-shop")
            val initialResult = CompletableDeferred<RamapResult<RamenShops>>()
            val latestResult = CompletableDeferred<RamapResult<RamenShops>>()
            val requestedShopIds = mutableListOf<Set<String>>()
            val ramenShopRepository =
                object : RamenShopRepository by FakeRamenShopRepository() {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        requestedShopIds += shopIds
                        return when (shopIds) {
                            setOf(initialShop.id) -> initialResult.await()
                            setOf(latestShop.id) -> latestResult.await()
                            else -> error("Unexpected shop ids: $shopIds")
                        }
                    }
                }
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(initialShop.id)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            assertEquals(true, viewModel.uiState.value.isOnlyLoading)

            personalizationRepository.updateBookmarkedShopIds(setOf(latestShop.id))
            runCurrent()
            latestResult.complete(RamapResult.Success(RamenShops(listOf(latestShop))))
            runCurrent()

            assertEquals(RamenShops(listOf(latestShop)), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)

            initialResult.complete(RamapResult.Success(RamenShops(listOf(initialShop))))
            runCurrent()

            assertEquals(RamenShops(listOf(latestShop)), viewModel.uiState.value.shops)
            assertEquals(
                listOf(setOf(initialShop.id), setOf(latestShop.id)),
                requestedShopIds,
            )
        }

    @Test
    fun `북마크가 비면 진행 중인 조회를 취소하고 늦은 결과를 반영하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val pendingResult = CompletableDeferred<RamapResult<RamenShops>>()
            val ramenShopRepository =
                object : RamenShopRepository by FakeRamenShopRepository() {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> = pendingResult.await()
                }
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            assertEquals(true, viewModel.uiState.value.isOnlyLoading)

            personalizationRepository.updateBookmarkedShopIds(emptySet())
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)

            pendingResult.complete(RamapResult.Success(RamenShops(listOf(shop))))
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
        }

    @Test
    fun `좋아요 해제에 성공하면 저장소와 현재 목록에서 매장을 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val repository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel = bookmarkedShopListViewModel(shop, repository)
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed(shop.id))

                assertEquals(
                    BookmarkedShopListSideEffect.ShowToast(
                        ToastData(
                            Res.string.bookmark_removal_success_message,
                            ToastType.SUCCESS,
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
                assertEquals(false, viewModel.uiState.value.isOverlayLoading)
                assertEquals(
                    RamapResult.Success(ShopPersonalization()),
                    repository.fetchPersonalization(),
                )
            }
        }

    @Test
    fun `좋아요 해제에 실패하면 목록을 유지하고 에러 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val repository =
                object : ShopPersonalizationStore by FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                ) {
                    override suspend fun updateBookmark(
                        shopId: String,
                        enabled: Boolean,
                    ): RamapResult<Unit> = RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
                }
            val viewModel = bookmarkedShopListViewModel(shop, repository)
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed(shop.id))

                assertEquals(
                    BookmarkedShopListSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
                assertEquals(false, viewModel.uiState.value.isOverlayLoading)
            }
        }
}

private fun bookmarkedShopListViewModel(
    shop: RamenShop,
    personalizationRepository: ShopPersonalizationStore =
        FakePersonalizationRepository(
            ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
        ),
) = BookmarkedShopListViewModel(
    personalizationStore = personalizationRepository,
    ramenShopRepository =
        FakeRamenShopRepository(
            fetchByIdsResult = RamenShops(listOf(shop)),
        ),
)
