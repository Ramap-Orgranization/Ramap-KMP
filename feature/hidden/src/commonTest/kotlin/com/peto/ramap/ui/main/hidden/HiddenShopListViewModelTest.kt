package com.peto.ramap.ui.main.hidden

import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.hidden.HiddenShopListViewModel
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
                    personalizationRepository =
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
            assertTrue(state is LoadState.Content)
            assertEquals(RamenShops(listOf(hiddenShop.copy(isVisible = false))), state.data)
        }

    @Test
    fun `숨긴 매장이 없으면 빈 목록을 표시한다`() =
        coroutinesTest {
            val viewModel =
                HiddenShopListViewModel(
                    personalizationRepository = FakePersonalizationRepository(),
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            runCurrent()

            assertEquals(LoadState.Content(RamenShops(emptyMap())), viewModel.uiState.value.shopsState)
        }
}
