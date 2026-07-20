package com.peto.ramap.ui.hidden

import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.common.RamapUiState
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
                            Personalization(hiddenShopIds = setOf(hiddenShop.id)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(hiddenShop.id to hiddenShop)),
                        ),
                )

            runCurrent()

            val state = viewModel.uiState.value.shopsState
            assertTrue(state is RamapUiState.Success<*>)
            assertEquals(RamenShops(listOf(hiddenShop.copy(isVisible = false))), state.data)
        }

    @Test
    fun `숨긴 매장이 없으면 빈 목록을 표시한다`() =
        coroutinesTest {
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            runCurrent()

            assertEquals(RamapUiState.Success(RamenShops(emptyMap())), viewModel.uiState.value.shopsState)
        }

    @Test
    fun `숨긴 매장 해제 성공시 목록에서 즉시 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-shop")
            val viewModel =
                HiddenShopListViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            Personalization(hiddenShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )
            runCurrent()

            viewModel.dispatch(HiddenShopListIntent.OnUnhideConfirmed(shop.id))
            runCurrent()

            assertEquals(
                RamapUiState.Success(RamenShops(emptyMap())),
                viewModel.uiState.value.shopsState,
            )
        }
}
