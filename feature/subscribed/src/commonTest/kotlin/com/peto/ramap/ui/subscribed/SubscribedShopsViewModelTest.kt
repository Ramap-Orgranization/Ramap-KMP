package com.peto.ramap.ui.subscribed

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SubscribedShopsViewModelTest {
    @Test
    fun `구독 매장 화면 진입시 구독 아이디에 해당하는 매장을 로드한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val viewModel =
                SubscribedShopsViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            Personalization(notificationShopIds = setOf(shop.id)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )

            runCurrent()

            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
        }

    @Test
    fun `매장 조회 실패 후 캐시된 구독 목록으로 동기화하면 오류 상태를 해제한다`() =
        coroutinesTest {
            val cachedShop = ramenShopFixture(id = "cached-subscribed-shop")
            val missingShop = ramenShopFixture(id = "missing-subscribed-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    Personalization(notificationShopIds = setOf(cachedShop.id)),
                )
            val ramenShopRepository =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(cachedShop)))
            val viewModel =
                SubscribedShopsViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            runCurrent()

            ramenShopRepository.error = RamapError.Unknown(IllegalStateException("failure"))
            personalizationRepository.updateShopNotification(missingShop.id, true)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showError)

            personalizationRepository.updateShopNotification(missingShop.id, false)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.showError)
            assertEquals(RamenShops(listOf(cachedShop)), viewModel.uiState.value.shops)
        }

    @Test
    fun `매장 조회 실패 후 재시도하면 구독 매장 목록을 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(listOf(shop)),
                    error = RamapError.Unknown(IllegalStateException("failure")),
                )
            val viewModel =
                SubscribedShopsViewModel(
                    ramenShopRepository = ramenShopRepository,
                    personalizationStore =
                        FakePersonalizationRepository(
                            Personalization(notificationShopIds = setOf(shop.id)),
                        ),
                )
            runCurrent()
            assertEquals(true, viewModel.uiState.value.showError)

            ramenShopRepository.error = null
            viewModel.dispatch(SubscribedShopsIntent.OnRetry)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.showError)
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
        }

    @Test
    fun `매장 알림을 모두 해제한 설정이 저장되면 해당 매장만 목록에서 제거한다`() =
        coroutinesTest {
            val clearedShop = ramenShopFixture(id = "cleared-shop")
            val remainingShop = ramenShopFixture(id = "remaining-shop")
            val personalizationStore =
                FakePersonalizationRepository(
                    Personalization(notificationShopIds = setOf(clearedShop.id, remainingShop.id)),
                )
            val viewModel =
                SubscribedShopsViewModel(
                    personalizationStore = personalizationStore,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(clearedShop, remainingShop))),
                )
            runCurrent()
            assertEquals(RamenShops(listOf(clearedShop, remainingShop)), viewModel.uiState.value.shops)

            val catalog = NotificationCatalog(revision = 1, types = emptyList())
            personalizationStore.publishNotificationSettings(
                NotificationSettings(
                    accountId = "account",
                    shopId = clearedShop.id,
                    catalog = catalog,
                    selection = catalog.clear(),
                ),
            )
            runCurrent()

            assertEquals(RamenShops(listOf(remainingShop)), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
        }
}
