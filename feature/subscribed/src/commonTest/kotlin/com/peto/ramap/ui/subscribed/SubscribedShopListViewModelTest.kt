package com.peto.ramap.ui.subscribed

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscribedShopListViewModelTest {
    @Test
    fun `구독 매장 화면 진입시 구독 아이디에 해당하는 매장을 로드한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val viewModel =
                SubscribedShopListViewModel(
                    notificationRepository =
                        FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)),
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )

            runCurrent()

            assertEquals(
                LoadState.Content(RamenShops(listOf(shop))),
                viewModel.uiState.value.shopsState,
            )
        }

    @Test
    fun `구독 매장 해제를 확인하면 목록과 저장소에서 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val repository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            val viewModel =
                SubscribedShopListViewModel(
                    notificationRepository = repository,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )
            runCurrent()

            viewModel.dispatch(
                SubscribedShopListIntent.OnRemovalRequested(
                    SubscribedRemovalTarget.Shop(shop.id),
                ),
            )
            viewModel.dispatch(SubscribedShopListIntent.OnRemovalConfirmed)
            runCurrent()

            assertEquals(LoadState.Content(RamenShops(emptyMap())), viewModel.uiState.value.shopsState)
            assertEquals(null, viewModel.uiState.value.pendingRemoval)
            assertTrue(repository.shopIds.isEmpty())
        }

    @Test
    fun `구독 매장 해제에 실패하면 목록을 유지하고 다이얼로그를 닫은 뒤 에러 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val repository =
                FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)).apply {
                    shopNotificationError =
                        RamapError.Unknown(IllegalStateException("failure"))
                }
            val viewModel =
                SubscribedShopListViewModel(
                    notificationRepository = repository,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    SubscribedShopListIntent.OnRemovalRequested(
                        SubscribedRemovalTarget.Shop(shop.id),
                    ),
                )
                viewModel.dispatch(SubscribedShopListIntent.OnRemovalConfirmed)

                assertEquals(
                    SubscribedShopListSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(null, viewModel.uiState.value.pendingRemoval)
                assertEquals(
                    LoadState.Content(RamenShops(listOf(shop))),
                    viewModel.uiState.value.shopsState,
                )
            }
        }
}
