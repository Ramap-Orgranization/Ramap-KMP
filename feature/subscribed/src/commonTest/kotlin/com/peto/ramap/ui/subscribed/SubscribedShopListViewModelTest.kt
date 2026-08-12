package com.peto.ramap.ui.subscribed

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
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
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(notificationShopIds = setOf(shop.id)),
                        ),
                    notificationRepository =
                        FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)),
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
                    ShopPersonalization(notificationShopIds = setOf(cachedShop.id)),
                )
            val ramenShopRepository =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(cachedShop)))
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = personalizationRepository,
                    notificationRepository = FakeNotificationSettingsRepository(),
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
    fun `활성화된 이벤트 알림 설정은 활성 이벤트를 로드한다`() =
        coroutinesTest {
            val event = shopEventFixture("enabled-event")
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository =
                        FakeNotificationSettingsRepository(
                            eventOverrides =
                                mutableListOf(EventNotificationOverride(event.id, true)),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(activeEvents = listOf(event)),
                )

            runCurrent()

            assertEquals(listOf(event), viewModel.uiState.value.subscribedEvents)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
        }

    @Test
    fun `비활성화되거나 존재하지 않는 이벤트 알림 설정은 목록에서 제외한다`() =
        coroutinesTest {
            val disabledEvent = shopEventFixture("disabled-event")
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository =
                        FakeNotificationSettingsRepository(
                            eventOverrides =
                                mutableListOf(
                                    EventNotificationOverride(disabledEvent.id, false),
                                    EventNotificationOverride("missing-event", true),
                                ),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(activeEvents = listOf(disabledEvent)),
                )

            runCurrent()

            assertTrue(
                viewModel.uiState.value.subscribedEvents
                    .isEmpty(),
            )
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
        }

    @Test
    fun `구독 매장 해제를 확인하면 목록과 저장소에서 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop")
            val repository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            val personalizationStore =
                FakePersonalizationRepository(
                    ShopPersonalization(notificationShopIds = setOf(shop.id)),
                )
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = personalizationStore,
                    notificationRepository = repository,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )
            runCurrent()

            viewModel.dispatch(
                SubscribedShopListIntent.OnRemovalConfirmed(
                    SubscribedRemovalTarget.Shop(shop.id),
                ),
            )
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(false, viewModel.uiState.value.isOverlayLoading)
            assertTrue(
                (personalizationStore.state.value as PersonalizationBootstrapState.Success)
                    .value
                    .notificationShopIds
                    .isEmpty(),
            )
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
            val personalizationStore =
                FakePersonalizationRepository(
                    ShopPersonalization(notificationShopIds = setOf(shop.id)),
                ).apply {
                    shopNotificationError =
                        RamapError.Unknown(IllegalStateException("failure"))
                }
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = personalizationStore,
                    notificationRepository = repository,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    SubscribedShopListIntent.OnRemovalConfirmed(
                        SubscribedRemovalTarget.Shop(shop.id),
                    ),
                )

                assertEquals(
                    SubscribedShopListSideEffect.ShowToast(
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

    @Test
    fun `이벤트 알림 설정 해제를 확인하면 목록과 저장소에서 제거한다`() =
        coroutinesTest {
            val event = shopEventFixture("subscribed-event")
            val repository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(event.id, true)),
                )
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository = repository,
                    ramenShopRepository = FakeRamenShopRepository(activeEvents = listOf(event)),
                )
            runCurrent()

            viewModel.dispatch(
                SubscribedShopListIntent.OnRemovalConfirmed(
                    SubscribedRemovalTarget.EventOverride(event.id),
                ),
            )
            runCurrent()

            assertTrue(
                viewModel.uiState.value.subscribedEvents
                    .isEmpty(),
            )
            assertTrue(repository.eventOverrides.isEmpty())
            assertEquals(listOf(event.id), repository.clearedEventNotificationIds)
        }

    @Test
    fun `이벤트 알림 설정 해제에 실패하면 이벤트를 유지하고 에러 토스트를 표시한다`() =
        coroutinesTest {
            val event = shopEventFixture("subscribed-event")
            val repository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(event.id, true)),
                ).apply {
                    clearEventNotificationOverrideError = failure()
                }
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository = repository,
                    ramenShopRepository = FakeRamenShopRepository(activeEvents = listOf(event)),
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    SubscribedShopListIntent.OnRemovalConfirmed(
                        SubscribedRemovalTarget.EventOverride(event.id),
                    ),
                )

                assertEquals(
                    SubscribedShopListSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(listOf(event), viewModel.uiState.value.subscribedEvents)
                assertEquals(listOf(event.id), repository.clearedEventNotificationIds)
            }
        }

    @Test
    fun `이벤트 알림 설정 조회 실패는 오류 상태로 표시하고 재시도한다`() =
        coroutinesTest {
            val repository =
                FakeNotificationSettingsRepository().apply {
                    fetchEventOverridesError = failure()
                }
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository = repository,
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            runCurrent()

            assertTrue(viewModel.uiState.value.showError)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)

            repository.fetchEventOverridesError = null
            viewModel.dispatch(SubscribedShopListIntent.OnRetry)
            runCurrent()

            assertTrue(!viewModel.uiState.value.showError)
            assertTrue(
                viewModel.uiState.value
                    .subscribedEvents
                    .isEmpty(),
            )
        }

    @Test
    fun `이벤트 상세 조회 실패는 오류 상태로 표시한다`() =
        coroutinesTest {
            val eventId = "failed-event"
            val viewModel =
                SubscribedShopListViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    notificationRepository =
                        FakeNotificationSettingsRepository(
                            eventOverrides =
                                mutableListOf(EventNotificationOverride(eventId, true)),
                        ),
                    ramenShopRepository =
                        FakeRamenShopRepository(activeEventError = failure()),
                )

            runCurrent()

            assertTrue(viewModel.uiState.value.showError)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertTrue(
                viewModel.uiState.value.subscribedEvents
                    .isEmpty(),
            )
        }

    private fun failure(): RamapError = RamapError.Unknown(IllegalStateException("failure"))

    private fun shopEventFixture(id: String) =
        ShopEvent(
            id = id,
            type = ShopEventType.POPUP,
            title = "팝업",
            description = "설명",
            startDate = "2099-07-15",
            endDate = "2099-07-16",
            sourceUrl = "https://instagram.com/event",
            isToday = false,
            isVenue = true,
            venueShop = ramenShopFixture(id = "shop", name = "매장", address = "서울"),
            waitingMethod = null,
            waitingUrl = null,
        )
}
