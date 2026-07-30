package com.peto.ramap.ui.hidden

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HiddenShopListViewModelTest {
    @Test
    fun `숨긴 매장 화면 진입시 숨긴 매장 목록을 로드한다`() =
        coroutinesTest {
            val hiddenShop = ramenShopFixture(id = "hidden-shop").copy(isVisible = true)
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(hiddenShopIds = setOf(hiddenShop.id)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(hiddenShop.id to hiddenShop)),
                        ),
                    analyticsTracker = FakeAnalyticsTracker(),
                )

            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(RamenShops(listOf(hiddenShop.copy(isVisible = false))), state.shops)
            assertTrue(!state.isOverlayLoading)
        }

    @Test
    fun `숨긴 매장이 없으면 빈 목록을 표시한다`() =
        coroutinesTest {
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    ramenShopRepository = FakeRamenShopRepository(),
                    analyticsTracker = FakeAnalyticsTracker(),
                )

            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertTrue(!viewModel.uiState.value.isOverlayLoading)
        }

    @Test
    fun `숨긴 매장 조회 실패 후 재시도하면 현재 아이디로 목록을 복구한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-shop")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(listOf(shop)),
                    error = RamapError.Unknown(IllegalStateException("failure")),
                )
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(hiddenShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository = ramenShopRepository,
                    analyticsTracker = FakeAnalyticsTracker(),
                )
            runCurrent()

            assertTrue(viewModel.uiState.value.showError)

            ramenShopRepository.error = null
            viewModel.dispatch(HiddenShopListIntent.OnRetry)
            runCurrent()

            assertTrue(!viewModel.uiState.value.showError)
            assertEquals(
                RamenShops(listOf(shop.copy(isVisible = false))),
                viewModel.uiState.value.shops,
            )
        }

    @Test
    fun `숨긴 매장 해제 성공시 목록에서 즉시 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-shop")
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(hiddenShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                    analyticsTracker = FakeAnalyticsTracker(),
                )
            runCurrent()

            viewModel.dispatch(HiddenShopListIntent.OnUnhideConfirmed(shop.id))
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertTrue(!viewModel.uiState.value.isOverlayLoading)
        }
}
