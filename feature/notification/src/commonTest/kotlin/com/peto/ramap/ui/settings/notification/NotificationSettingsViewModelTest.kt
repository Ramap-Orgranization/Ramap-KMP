package com.peto.ramap.ui.settings.notification

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnNotificationSettingsRetried
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnShopRemoved
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    private val failure = RamapError.Unknown(IllegalStateException("failure"))

    @Test
    fun `서버 설정이 켜져 있으면 전체 알림 서버 상태를 켠 상태로 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = true),
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            runCurrent()

            assertTrue(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `서버 설정이 꺼져 있으면 전체 알림 서버 상태를 끈 상태로 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = false),
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            runCurrent()

            assertFalse(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `초기 설정 조회 중 하나라도 실패하면 부분 상태를 표시하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val repositories =
                listOf(
                    FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)).apply {
                        fetchEnabledError = failure
                    },
                    FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)).apply {
                        fetchShopIdsError = failure
                    },
                    FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id)).apply {
                        fetchEventOverridesError = failure
                    },
                )

            repositories.forEach { notificationRepository ->
                val viewModel =
                    NotificationSettingsViewModel(
                        notificationRepository = notificationRepository,
                        ramenShopRepository =
                            FakeRamenShopRepository(
                                fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                            ),
                    )
                runCurrent()

                assertEquals(LoadState.Error, viewModel.uiState.value.loadState)
            }
        }

    @Test
    fun `구독 매장 조회에 실패하면 초기 설정 전체를 표시하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository =
                        FakeNotificationSettingsRepository(
                            shopIds = mutableSetOf(shop.id),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(error = failure),
                )

            runCurrent()

            assertEquals(LoadState.Error, viewModel.uiState.value.loadState)
        }

    @Test
    fun `이벤트 상세 조회에 실패하면 초기 설정 전체를 표시하지 않는다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository =
                        FakeNotificationSettingsRepository(
                            eventOverrides = mutableListOf(EventNotificationOverride("event-id", true)),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(activeEventError = failure),
                )

            runCurrent()

            assertEquals(LoadState.Error, viewModel.uiState.value.loadState)
        }

    @Test
    fun `초기 설정 재시도에 성공하면 콘텐츠를 표시한다`() =
        coroutinesTest {
            val notificationRepository =
                FakeNotificationSettingsRepository(enabled = true).apply {
                    fetchEnabledError = failure
                }
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository = FakeRamenShopRepository(),
                )
            runCurrent()
            assertEquals(LoadState.Error, viewModel.uiState.value.loadState)

            notificationRepository.fetchEnabledError = null
            viewModel.dispatch(OnNotificationSettingsRetried)
            runCurrent()

            assertEquals(LoadState.Content(Unit), viewModel.uiState.value.loadState)
            assertTrue(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `구독 매장을 해제하면 알림 설정 목록에서 제거한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val notificationRepository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                )
            runCurrent()
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)

            viewModel.dispatch(OnShopRemoved(shop.id))
            runCurrent()

            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertTrue(notificationRepository.shopIds.isEmpty())
        }

    @Test
    fun `구독 매장 해제에 실패하면 목록과 공유 알림 상태를 유지한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val notificationRepository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            notificationRepository.shopNotificationError = RamapError.Unknown(IllegalStateException("failure"))
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                )
            runCurrent()

            viewModel.dispatch(OnShopRemoved(shop.id))
            runCurrent()

            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.shops)
            assertEquals(setOf(shop.id), notificationRepository.subscribedShopIds.value)
        }

    @Test
    fun `구독 매장 해제를 취소하면 알림 설정을 유지한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val notificationRepository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository = FakeRamenShopRepository(),
                )
            runCurrent()

            viewModel.dispatch(OnRemovalRequested(NotificationRemovalTarget.Shop(shop.id)))
            viewModel.dispatch(OnRemovalDismissed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.pendingRemoval)
            assertEquals(setOf(shop.id), notificationRepository.shopIds)
        }

    @Test
    fun `구독 매장 해제를 확인하면 알림 설정을 해제한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val notificationRepository = FakeNotificationSettingsRepository(shopIds = mutableSetOf(shop.id))
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository = FakeRamenShopRepository(),
                )
            runCurrent()

            viewModel.dispatch(OnRemovalRequested(NotificationRemovalTarget.Shop(shop.id)))
            viewModel.dispatch(OnRemovalConfirmed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.pendingRemoval)
            assertTrue(notificationRepository.shopIds.isEmpty())
        }
}
