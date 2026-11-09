package com.peto.ramap.ui.onboarding

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.contract.ShopSelectionSideEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_completion_failure
import ramap.shared.generated.resources.personalization_update_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopSelectionViewModelTest {
    private val shops = RamenShops((1..4).map { ramenShopFixture(id = "shop-$it", name = "라멘 $it") })

    @Test
    fun `기존 비회원 저장을 선택 상태로 복원하고 두 곳으로는 완료할 수 없다`() =
        coroutinesTest {
            val savedIds = setOf("shop-1", "shop-2")
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = savedIds))
            val storage = FakeOnboardingStorage()
            val login = FakeLoginRepository()
            val viewModel = createViewModel(store = store, storage = storage, login = login)
            runCurrent()

            assertEquals(2, viewModel.uiState.value.bookmarks.savedCount)
            assertTrue("shop-1" in viewModel.uiState.value.bookmarks)
            assertEquals(4, viewModel.uiState.value.shops.size)
            assertFalse(viewModel.uiState.value.canComplete)
            viewModel.dispatch(ShopSelectionIntent.Complete)
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("unknown"))
            runCurrent()
            assertFalse(storage.completed)
            assertTrue(store.bookmarkUpdateRequests.isEmpty())
            assertEquals(0, login.signInWithKakaoCallCount + login.signInWithAppleCallCount)
        }

    @Test
    fun `세 번째 저장은 즉시 반영하고 재진입해도 유지되지만 시작 전에는 완료하지 않는다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-1", "shop-2")))
            val storage = FakeOnboardingStorage()
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3"))
            runCurrent()
            assertTrue(viewModel.uiState.value.canComplete)
            assertEquals(setOf("shop-1", "shop-2", "shop-3"), store.bookmarkedShopIds.value)
            assertFalse(storage.completed)

            val reopened = createViewModel(store = store, storage = storage)
            runCurrent()
            assertEquals(3, reopened.uiState.value.bookmarks.savedCount)
            reopened.sideEffect.test {
                reopened.dispatch(ShopSelectionIntent.Complete)
                assertEquals(ShopSelectionSideEffect.Completed, awaitItem())
                assertTrue(storage.completed)
            }
        }

    @Test
    fun `네 곳 이상도 저장할 수 있고 해제하여 두 곳이 되면 진행을 막는다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val viewModel = createViewModel(store = store)
            runCurrent()
            assertTrue(viewModel.uiState.value.canComplete)
            assertEquals(0, viewModel.uiState.value.bookmarks.remainingCount)
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-4"))
            runCurrent()
            assertTrue(viewModel.uiState.value.canComplete)
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3"))
            runCurrent()
            assertFalse(viewModel.uiState.value.canComplete)
            assertEquals(1, viewModel.uiState.value.bookmarks.remainingCount)
        }

    @Test
    fun `저장 실패는 오류 토스트 한 번과 선택 유지로 안내하고 재선택으로 복구한다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-1", "shop-2")))
            store.bookmarkUpdateError = RamapError.Unknown(IllegalStateException())
            val storage = FakeOnboardingStorage()
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            viewModel.sideEffect.test {
                repeat(2) { viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3")) }
                assertEquals(
                    ShopSelectionSideEffect.ShowToast(ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR)),
                    awaitItem(),
                )
                runCurrent()
                expectNoEvents()
                assertEquals(2, viewModel.uiState.value.bookmarks.savedCount)
                assertFalse("shop-3" in viewModel.uiState.value.bookmarks)
                assertFalse(viewModel.uiState.value.canComplete)
                assertFalse(storage.completed)

                store.bookmarkUpdateError = null
                viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3"))
                runCurrent()
                assertTrue(viewModel.uiState.value.canComplete)
                assertTrue("shop-3" in viewModel.uiState.value.bookmarks)
                expectNoEvents()
            }
        }

    @Test
    fun `저장 해제 실패도 오류 토스트를 보내고 기존 선택과 시작 가능 상태를 유지한다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-1", "shop-2", "shop-3")))
            store.bookmarkUpdateError = RamapError.Unknown(IllegalStateException())
            val viewModel = createViewModel(store = store)
            runCurrent()
            viewModel.sideEffect.test {
                viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3"))
                assertEquals(
                    ShopSelectionSideEffect.ShowToast(ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR)),
                    awaitItem(),
                )
                runCurrent()
                assertEquals(3, viewModel.uiState.value.bookmarks.savedCount)
                assertTrue("shop-3" in viewModel.uiState.value.bookmarks)
                assertTrue(viewModel.uiState.value.canComplete)
                expectNoEvents()
            }
        }

    @Test
    fun `세 곳에서 해제 저장이 진행 중이면 중복 입력과 완료를 막는다`() =
        coroutinesTest {
            val store = ControllablePersonalizationStore(FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-1", "shop-2", "shop-3"))))
            val pending = CompletableDeferred<Unit>()
            store.pendingUpdate = pending
            val storage = FakeOnboardingStorage()
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            repeat(2) { viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-3")) }
            viewModel.dispatch(ShopSelectionIntent.Complete)
            runCurrent()
            assertEquals(3, viewModel.uiState.value.bookmarks.savedCount)
            assertFalse(viewModel.uiState.value.canComplete)
            assertFalse(storage.completed)
            pending.complete(Unit)
            runCurrent()
            assertEquals(2, viewModel.uiState.value.bookmarks.savedCount)
            assertEquals(listOf("shop-3" to false), store.delegate.bookmarkUpdateRequests)
        }

    @Test
    fun `시작하기 실패는 오류 토스트 한 번과 선택 유지로 안내하고 재시도할 수 있다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val storage = FakeOnboardingStorage().apply { completionFailure = IllegalStateException() }
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            viewModel.sideEffect.test {
                repeat(2) { viewModel.dispatch(ShopSelectionIntent.Complete) }
                assertEquals(
                    ShopSelectionSideEffect.ShowToast(ToastData(Res.string.onboarding_completion_failure, ToastType.ERROR)),
                    awaitItem(),
                )
                runCurrent()
                expectNoEvents()
                assertFalse(storage.completed)
                assertTrue(viewModel.uiState.value.canComplete)
                assertEquals(shops.size, viewModel.uiState.value.bookmarks.savedCount)
                storage.completionFailure = null
                viewModel.dispatch(ShopSelectionIntent.Complete)
                assertEquals(ShopSelectionSideEffect.Completed, awaitItem())
                assertTrue(storage.completed)
            }
        }

    @Test
    fun `시작하기 작업 취소는 오류 토스트나 완료 이벤트를 보내지 않는다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val storage = FakeOnboardingStorage().apply { completionFailure = CancellationException() }
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            viewModel.sideEffect.test {
                viewModel.dispatch(ShopSelectionIntent.Complete)
                runCurrent()
                expectNoEvents()
                assertFalse(storage.completed)
                assertTrue(viewModel.uiState.value.canComplete)
            }
        }

    @Test
    fun `최초 매장 조회 실패 후 재시도하면 오류를 지우고 목록을 표시한다`() =
        coroutinesTest {
            val ranking = FakeOnboardingRankingRepository(shops).apply { error = RamapError.Unknown(IllegalStateException()) }
            val viewModel = createViewModel(ranking = ranking)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasShopsError)
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isLoadingShops)

            ranking.error = null
            viewModel.dispatch(ShopSelectionIntent.RetryShops)
            runCurrent()
            assertFalse(viewModel.uiState.value.hasShopsError)
            assertEquals(shops.keys, viewModel.uiState.value.shops.keys)
            assertFalse(viewModel.uiState.value.isLoadingShops)
        }

    @Test
    fun `완료 기록 저장 중에는 선택 변경을 막고 성공 이벤트는 한 번만 보낸다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val pending = CompletableDeferred<Unit>()
            val storage = FakeOnboardingStorage().apply { pendingCompletion = pending }
            val viewModel = createViewModel(store = store, storage = storage)
            runCurrent()
            viewModel.sideEffect.test {
                repeat(2) { viewModel.dispatch(ShopSelectionIntent.Complete) }
                viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-1"))
                runCurrent()
                assertTrue(viewModel.uiState.value.isCompleting)
                assertTrue(store.bookmarkUpdateRequests.isEmpty())
                expectNoEvents()
                pending.complete(Unit)
                assertEquals(ShopSelectionSideEffect.Completed, awaitItem())
                viewModel.dispatch(ShopSelectionIntent.Complete)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `세션 복원이 끝나기 전에는 이전 개인화 상태로 저장하거나 완료하지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<Unit>()
            val login =
                object : LoginRepository by FakeLoginRepository() {
                    override suspend fun awaitInitialization() {
                        pending.await()
                    }
                }
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val viewModel = createViewModel(store = store, login = login)
            runCurrent()
            assertFalse(viewModel.uiState.value.canComplete)
            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(ShopSelectionLoadKey.Personalization),
            )
            pending.complete(Unit)
            runCurrent()
            assertTrue(viewModel.uiState.value.canComplete)
        }

    @Test
    fun `기기 저장 조회 실패는 재시도로 복구한다`() =
        coroutinesTest {
            val store = ControllablePersonalizationStore(FakePersonalizationRepository())
            store.refreshError = RamapError.Unknown(IllegalStateException())
            val viewModel = createViewModel(store = store)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasPersonalizationError)
            assertFalse(viewModel.uiState.value.canChangeBookmarks)
            store.refreshError = null
            viewModel.dispatch(ShopSelectionIntent.RetryPersonalization)
            runCurrent()
            assertFalse(viewModel.uiState.value.hasPersonalizationError)
            assertTrue(viewModel.uiState.value.canChangeBookmarks)
        }

    @Test
    fun `인기 목록 실패해도 검색한 매장을 저장할 수 있고 검색 해제 후에도 선택은 유지한다`() =
        coroutinesTest {
            val ranking = FakeOnboardingRankingRepository(shops).apply { error = RamapError.Unknown(IllegalStateException()) }
            val searchedShop = ramenShopFixture(id = "searched-shop")
            val repository = FakeRamenShopRepository(searchResult = RamenShops(listOf(searchedShop)))
            val viewModel = createViewModel(repository = repository, ranking = ranking)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasShopsError)
            viewModel.dispatch(ShopSelectionIntent.QueryChanged("  라멘  "))
            advanceUntilIdle()
            assertEquals("라멘", repository.requestedSearchQueries.single().value)
            assertEquals(setOf("searched-shop"), viewModel.uiState.value.shops.keys)
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("searched-shop"))
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.QueryChanged(""))
            runCurrent()
            assertTrue("searched-shop" in viewModel.uiState.value.bookmarks)
            assertTrue("searched-shop" in viewModel.uiState.value.suggestedShops)
        }

    @Test
    fun `검색어를 지우면 진행 중 검색의 늦은 결과가 인기 목록을 덮어쓰지 않는다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(searchResult = RamenShops(listOf(ramenShopFixture(id = "search"))), searchDelayMillis = 1000)
            val viewModel = createViewModel(repository = repository)
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.QueryChanged("라멘"))
            advanceTimeBy(400)
            viewModel.dispatch(ShopSelectionIntent.QueryChanged(""))
            advanceUntilIdle()
            assertEquals(shops.keys, viewModel.uiState.value.shops.keys)
            assertTrue(
                viewModel.uiState.value.searchResults
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isLoadingShops)
        }

    @Test
    fun `이미 세 곳이 저장되어 있으면 목록 네트워크 오류가 있어도 시작할 수 있다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = shops.keys))
            val ranking = FakeOnboardingRankingRepository(shops).apply { error = RamapError.Unknown(IllegalStateException()) }
            val viewModel = createViewModel(store = store, ranking = ranking)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasShopsError)
            assertTrue(viewModel.uiState.value.canComplete)
        }

    private fun createViewModel(
        store: PersonalizationStore = FakePersonalizationRepository(),
        storage: FakeOnboardingStorage = FakeOnboardingStorage(),
        login: LoginRepository = FakeLoginRepository(),
        repository: FakeRamenShopRepository = FakeRamenShopRepository(fetchByIdsResult = shops),
        ranking: FakeOnboardingRankingRepository = FakeOnboardingRankingRepository(shops),
    ): ShopSelectionViewModel = ShopSelectionViewModel(SearchRamenShopsUseCase(repository), repository, ranking, store, login, storage)
}
