package com.peto.ramap.ui.bookmark

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.common.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkedShopListViewModelTest {
    @Test
    fun `좋아요 매장 화면 진입시 개인화 아이디에 해당하는 매장을 로드한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationRepository =
                        FakePersonalizationRepository(
                            Personalization(bookmarkedShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(listOf(shop)),
                        ),
                )

            runCurrent()

            assertEquals(
                LoadState.Content(RamenShops(listOf(shop))),
                viewModel.uiState.value.shopsState,
            )
        }

    @Test
    fun `공유 북마크 상태의 제거와 추가를 목록에 동기화한다`() =
        coroutinesTest {
            val initialShop = ramenShopFixture(id = "initial-bookmarked-shop", name = "기존 매장")
            val addedShop = ramenShopFixture(id = "added-bookmarked-shop", name = "추가 매장")
            val personalizationRepository =
                FakePersonalizationRepository(
                    Personalization(bookmarkedShopIds = setOf(initialShop.id)),
                )
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(listOf(initialShop, addedShop)),
                )
            val viewModel =
                BookmarkedShopListViewModel(
                    personalizationRepository = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            personalizationRepository.updateBookmarkedShopIds(emptySet())
            runCurrent()

            assertEquals(
                LoadState.Content(RamenShops(emptyMap())),
                viewModel.uiState.value.shopsState,
            )

            personalizationRepository.updateBookmarkedShopIds(setOf(addedShop.id))
            runCurrent()

            assertEquals(
                LoadState.Content(RamenShops(listOf(addedShop))),
                viewModel.uiState.value.shopsState,
            )
            assertEquals(
                listOf(setOf(initialShop.id), setOf(addedShop.id)),
                ramenShopRepository.requestedShopIdsHistory,
            )
        }

    @Test
    fun `좋아요 매장을 선택하면 좋아요 해제 확인 대상을 저장한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val viewModel = bookmarkedShopListViewModel(shop)
            runCurrent()

            viewModel.dispatch(BookmarkedShopListIntent.OnShopClicked(shop.id))
            runCurrent()

            assertEquals(shop.id, viewModel.uiState.value.pendingBookmarkShopId)
        }

    @Test
    fun `좋아요 해제를 취소하면 저장소를 호출하지 않고 확인 대상을 비운다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            var removalCount = 0
            val repository =
                object : PersonalizationRepository by FakePersonalizationRepository(
                    Personalization(bookmarkedShopIds = setOf(shop.id)),
                ) {
                    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> {
                        removalCount += 1
                        return RamapResult.Success(Unit)
                    }
                }
            val viewModel = bookmarkedShopListViewModel(shop, repository)
            runCurrent()

            viewModel.dispatch(BookmarkedShopListIntent.OnShopClicked(shop.id))
            viewModel.dispatch(BookmarkedShopListIntent.OnRemovalDismissed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.pendingBookmarkShopId)
            assertEquals(0, removalCount)
            assertEquals(
                LoadState.Content(RamenShops(listOf(shop))),
                viewModel.uiState.value.shopsState,
            )
        }

    @Test
    fun `좋아요 해제에 성공하면 저장소와 현재 목록에서 매장을 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val repository =
                FakePersonalizationRepository(
                    Personalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel = bookmarkedShopListViewModel(shop, repository)
            runCurrent()

            viewModel.dispatch(BookmarkedShopListIntent.OnShopClicked(shop.id))
            viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.pendingBookmarkShopId)
            assertEquals(
                LoadState.Content(RamenShops(emptyMap())),
                viewModel.uiState.value.shopsState,
            )
            assertEquals(
                RamapResult.Success(Personalization()),
                repository.fetchPersonalization(),
            )
        }

    @Test
    fun `좋아요 해제에 실패하면 목록을 유지하고 에러 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "bookmarked-shop")
            val repository =
                object : PersonalizationRepository by FakePersonalizationRepository(
                    Personalization(bookmarkedShopIds = setOf(shop.id)),
                ) {
                    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> =
                        RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
                }
            val viewModel = bookmarkedShopListViewModel(shop, repository)
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(BookmarkedShopListIntent.OnShopClicked(shop.id))
                viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed)

                assertEquals(
                    BookmarkedShopListSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(null, viewModel.uiState.value.pendingBookmarkShopId)
                assertEquals(
                    LoadState.Content(RamenShops(listOf(shop))),
                    viewModel.uiState.value.shopsState,
                )
            }
        }
}

private fun bookmarkedShopListViewModel(
    shop: RamenShop,
    personalizationRepository: PersonalizationRepository =
        FakePersonalizationRepository(
            Personalization(bookmarkedShopIds = setOf(shop.id)),
        ),
) = BookmarkedShopListViewModel(
    personalizationRepository = personalizationRepository,
    ramenShopRepository =
        FakeRamenShopRepository(
            fetchByIdsResult = RamenShops(listOf(shop)),
        ),
)
