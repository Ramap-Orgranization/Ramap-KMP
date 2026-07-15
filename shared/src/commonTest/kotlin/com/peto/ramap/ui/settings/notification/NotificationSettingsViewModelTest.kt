package com.peto.ramap.ui.settings.notification

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.settings.notification.contract.NotificationRemovalTarget
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnResumed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnShopRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect.ShowToast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @Test
    fun `서버 설정이 켜져 있어도 알림 권한이 없으면 전체 알림을 끈 상태로 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = true),
                    ramenShopRepository = FakeRamenShopRepository(),
                    isNotificationPermissionGranted = { false },
                )

            runCurrent()

            assertFalse(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `서버 설정과 알림 권한이 모두 켜져 있으면 전체 알림을 켠 상태로 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = true),
                    ramenShopRepository = FakeRamenShopRepository(),
                    isNotificationPermissionGranted = { true },
                )

            runCurrent()

            assertTrue(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `알림 권한을 시스템 설정에서 허용하고 돌아오면 전체 알림 상태를 갱신한다`() =
        coroutinesTest {
            var isPermissionGranted = false
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = true),
                    ramenShopRepository = FakeRamenShopRepository(),
                    isNotificationPermissionGranted = { isPermissionGranted },
                )
            runCurrent()
            assertFalse(viewModel.uiState.value.areEnabled)

            isPermissionGranted = true
            viewModel.dispatch(OnResumed)
            runCurrent()

            assertTrue(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `알림 권한이 있어도 서버 설정이 꺼져 있으면 전체 알림을 끈 상태로 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = FakeNotificationSettingsRepository(enabled = false),
                    ramenShopRepository = FakeRamenShopRepository(),
                    isNotificationPermissionGranted = { true },
                )

            runCurrent()

            assertFalse(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `알림 권한이 거부되면 전체 알림을 저장하지 않고 설정 안내를 보여준다`() =
        coroutinesTest {
            val notificationRepository = FakeNotificationSettingsRepository(enabled = false)
            val viewModel =
                NotificationSettingsViewModel(
                    notificationRepository = notificationRepository,
                    ramenShopRepository = FakeRamenShopRepository(),
                    requestNotificationPermission = { false },
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnEnabledChanged(true))
                runCurrent()

                assertFalse(viewModel.uiState.value.areEnabled)
                assertEquals(emptyList(), notificationRepository.enabledUpdates)
                assertEquals(
                    ShowToast(
                        ToastData(
                            message = Res.string.notification_permission_enable_message,
                            type = ToastType.DEFAULT,
                            action = ToastAction(label = Res.string.location_permission_settings_action),
                        ),
                    ),
                    awaitItem(),
                )
            }
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
            assertEquals(listOf(shop), viewModel.uiState.value.shops)

            viewModel.dispatch(OnShopRemoved(shop.id))
            runCurrent()

            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertTrue(notificationRepository.shopIds.isEmpty())
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
