package com.peto.ramap.ui.shop.overview

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.notification.NotificationTypeId
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_notification_settings_save_success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @Test
    fun `서버에서 새로 추가한 항목을 순서와 제목대로 읽고 선택해 저장한다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            val settings = assertNotNull(fixture.viewModel.uiState.value.notificationSettings)
            assertEquals(listOf("이벤트", "새 서버 항목"), settings.catalog.types.map { it.title })
            assertTrue(settings.selection.ids.isEmpty())
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                expectNoEvents()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationTypeToggled(NotificationTypeId("future-server-type")))
                runCurrent()
                val permission = awaitItem()
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                assertTrue(fixture.notificationSaveRequests.isEmpty())
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(permission.generation, true))
                runCurrent()
                assertTrue(fixture.notificationSaveRequests.isEmpty())
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                expectNoEvents()
                assertEquals(setOf(NotificationTypeId("future-server-type")), fixture.notificationSaveRequests.single().ids)
                assertNull(fixture.viewModel.uiState.value.notificationSettingsShopId)
                assertTrue("shop" in fixture.viewModel.uiState.value.notificationShopIds)
            }
        }

    @Test
    fun `알림 설정 저장 성공 시 성공 토스트를 보여준다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.Interaction>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                val effect = awaitItem()
                val toast = assertIs<ShopInteractionEffect.ShowToast>(effect.effect)
                assertEquals(Res.string.shop_notification_settings_save_success, toast.data.message)
            }
        }

    @Test
    fun `전체 선택과 전체 해제는 초안만 바꾸고 취소하면 저장하지 않는다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
            runCurrent()
            assertEquals(fixture.notificationCatalog.selectAll(), fixture.viewModel.uiState.value.notificationDraft)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
            runCurrent()
            assertEquals(fixture.notificationCatalog.clear(), fixture.viewModel.uiState.value.notificationDraft)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationCancel)
            runCurrent()
            assertNull(fixture.viewModel.uiState.value.notificationDraft)
            assertTrue(fixture.notificationSaveRequests.isEmpty())
        }

    @Test
    fun `조회 실패는 빈 설정이 아니며 저장 요청을 보내지 않는다`() =
        settingsScenario { fixture ->
            fixture.fetchNotificationSettings = { _, _ -> RamapResult.Error(RamapError.Http(500)) }
            openSettings(fixture)
            assertTrue(fixture.viewModel.uiState.value.hasNotificationLoadFailure)
            assertNull(fixture.viewModel.uiState.value.notificationSettings)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
            runCurrent()
            assertTrue(fixture.notificationSaveRequests.isEmpty())
        }

    @Test
    fun `권한을 거절해도 초안을 유지하고 다시 묻지 않으며 저장할 수 있다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                runCurrent()
                val permission = awaitItem()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(permission.generation, false))
                runCurrent()
                assertEquals(fixture.notificationCatalog.selectAll(), fixture.viewModel.uiState.value.notificationDraft)
                assertFalse(assertNotNull(fixture.viewModel.uiState.value.isNotificationPermissionGranted))
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                assertTrue(fixture.notificationSaveRequests.isEmpty())
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                runCurrent()
                expectNoEvents()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                expectNoEvents()
                assertEquals(listOf(fixture.notificationCatalog.selectAll()), fixture.notificationSaveRequests)
                assertNull(fixture.viewModel.uiState.value.notificationSettingsShopId)
            }
        }

    @Test
    fun `기존 이벤트 선택은 유지되며 전체 해제 저장에는 권한을 요청하지 않는다`() =
        settingsScenario { fixture ->
            fixture.fetchNotificationSettings = { shopId, accountId ->
                RamapResult.Success(NotificationSettings(accountId, shopId, fixture.notificationCatalog, fixture.notificationCatalog.selection(setOf(NotificationTypeId("event")))))
            }
            openSettings(fixture)
            assertEquals(
                setOf(NotificationTypeId("event")),
                fixture.viewModel.uiState.value.notificationDraft
                    ?.ids,
            )
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertTrue(
                    fixture.notificationSaveRequests
                        .single()
                        .ids
                        .isEmpty(),
                )
                expectNoEvents()
            }
        }

    @Test
    fun `저장 실패는 토스트로 알리고 초안을 유지하며 카탈로그 충돌 후 재저장은 막는다`() =
        settingsScenario { fixture ->
            fixture.fetchNotificationSettings = { shopId, accountId ->
                RamapResult.Success(NotificationSettings(accountId, shopId, fixture.notificationCatalog, fixture.notificationCatalog.selectAll()))
            }
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationTypeToggled(NotificationTypeId("future-server-type")))
            runCurrent()
            val draft = fixture.viewModel.uiState.value.notificationDraft
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.ShowNotificationSaveFailure>().test {
                fixture.saveNotificationSettings = { _, _ -> RamapResult.Error(RamapError.Http(500)) }
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertEquals(ShopOverviewSideEffect.ShowNotificationSaveFailure, awaitItem())
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                assertEquals("shop", fixture.viewModel.uiState.value.notificationSettingsShopId)
                assertEquals(setOf(NotificationTypeId("event")), draft?.ids)
                assertEquals(draft, fixture.viewModel.uiState.value.notificationDraft)
                fixture.saveNotificationSettings = { _, _ -> RamapResult.Error(RamapError.Http(409)) }
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertTrue(fixture.viewModel.uiState.value.hasNotificationRevisionMismatch)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertEquals(2, fixture.notificationSaveRequests.size)
                assertFalse("shop" in fixture.viewModel.uiState.value.notificationShopIds)
                expectNoEvents()
            }
        }

    @Test
    fun `매장 전환 뒤 늦은 설정 조회는 새 매장에 반영하지 않는다`() = lateLoadScenario(changeAccount = false)

    @Test
    fun `계정 전환 뒤 늦은 설정 조회는 새 계정에 반영하지 않는다`() = lateLoadScenario(changeAccount = true)

    @Test
    fun `매장 전환 뒤 늦은 설정 저장은 종 상태를 바꾸지 않는다`() = lateSaveScenario(changeAccount = false)

    @Test
    fun `계정 전환 뒤 늦은 설정 저장은 종 상태를 바꾸지 않는다`() = lateSaveScenario(changeAccount = true)

    @Test
    fun `일반 저장은 숨겨진 유효 구독을 보존하며 전체 해제 저장만 제거한다`() =
        settingsScenario { fixture ->
            fixture.fetchNotificationSettings = { shopId, accountId ->
                RamapResult.Success(NotificationSettings(accountId, shopId, fixture.notificationCatalog, fixture.notificationCatalog.clear(), hasHiddenEffectiveSubscription = true))
            }
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
            runCurrent()
            assertEquals(listOf(false), fixture.notificationClearAllSaveRequests)
            assertEquals(setOf("shop"), fixture.viewModel.uiState.value.notificationShopIds)

            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
            runCurrent()
            assertEquals(listOf(false, true), fixture.notificationClearAllSaveRequests)
            assertTrue(
                fixture.viewModel.uiState.value.notificationShopIds
                    .isEmpty(),
            )
        }

    @Test
    fun `전체 해제 뒤 다시 선택해도 숨김 선택 제거 의도는 저장까지 유지한다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationTypeToggled(NotificationTypeId("event")))
            runCurrent()
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                val permission = awaitItem()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(permission.generation, true))
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertEquals(listOf(true), fixture.notificationClearAllSaveRequests)
                assertEquals(setOf(NotificationTypeId("event")), fixture.notificationSaveRequests.single().ids)
            }
        }

    @Test
    fun `전체 해제 초안을 취소하고 다시 열면 숨김 선택 제거 의도를 버린다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationClear)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationCancel)
            runCurrent()
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
            runCurrent()
            assertEquals(listOf(false), fixture.notificationClearAllSaveRequests)
        }

    @Test
    fun `이미 권한이 허용되어 있으면 항목을 켜거나 저장할 때 권한을 요청하지 않는다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionChecked(true))
            runCurrent()
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                expectNoEvents()
                assertEquals(listOf(fixture.notificationCatalog.selectAll()), fixture.notificationSaveRequests)
            }
        }

    @Test
    fun `권한 응답을 기다리는 중에도 저장은 권한 응답과 별개로 완료된다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                runCurrent()
                val permission = awaitItem()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                assertEquals(listOf(fixture.notificationCatalog.selectAll()), fixture.notificationSaveRequests)
                assertNull(fixture.viewModel.uiState.value.notificationSettingsShopId)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(permission.generation, true))
                runCurrent()
                assertEquals(1, fixture.notificationSaveRequests.size)
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
            }
        }

    @Test
    fun `설정창을 다시 연 뒤 이전 권한 응답은 새 요청을 완료하지 않는다`() =
        settingsScenario { fixture ->
            openSettings(fixture)
            fixture.viewModel.sideEffect.filterIsInstance<ShopOverviewSideEffect.RequestNotificationPermission>().test {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                runCurrent()
                val previous = awaitItem()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationCancel)
                runCurrent()
                openSettings(fixture)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
                runCurrent()
                val current = awaitItem()
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationPermissionResult(previous.generation, true))
                runCurrent()
                assertEquals(current.generation, fixture.viewModel.uiState.value.notificationPermissionGeneration)
                assertNull(fixture.viewModel.uiState.value.isNotificationPermissionGranted)
                assertTrue(fixture.notificationSaveRequests.isEmpty())
            }
        }

    private fun lateLoadScenario(changeAccount: Boolean) =
        settingsScenario { fixture ->
            val response = CompletableDeferred<Unit>()
            fixture.awaitNotificationResponse = { withContext(NonCancellable) { response.await() } }
            try {
                openSettings(fixture)
                changeContext(fixture, changeAccount)
                response.complete(Unit)
                runCurrent()
                assertNull(fixture.viewModel.uiState.value.notificationSettings)
                assertNull(fixture.viewModel.uiState.value.notificationDraft)
            } finally {
                response.complete(Unit)
            }
        }

    private fun lateSaveScenario(changeAccount: Boolean) =
        settingsScenario { fixture ->
            val response = CompletableDeferred<Unit>()
            openSettings(fixture)
            fixture.saveNotificationSettings = { settings, _ ->
                withContext(NonCancellable) { response.await() }
                RamapResult.Success(settings.copy(selection = settings.catalog.selectAll()))
            }
            try {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSave)
                runCurrent()
                changeContext(fixture, changeAccount)
                response.complete(Unit)
                runCurrent()
                assertTrue(
                    fixture.viewModel.uiState.value.notificationShopIds
                        .isEmpty(),
                )
                assertNull(fixture.viewModel.uiState.value.notificationSettingsShopId)
            } finally {
                response.complete(Unit)
            }
        }

    private fun TestScope.changeContext(
        fixture: OverviewViewModelFixture,
        changeAccount: Boolean,
    ) {
        if (changeAccount) {
            fixture.login.updateAccountId("account-b")
        } else {
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected("other"))
        }
        runCurrent()
    }

    private fun TestScope.openSettings(fixture: OverviewViewModelFixture) {
        fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(ramenShopFixture(id = "shop")))
        runCurrent()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun settingsScenario(block: suspend TestScope.(OverviewViewModelFixture) -> Unit) =
    coroutinesTest {
        val fixture = OverviewViewModelFixture(LoginSessionState.AUTHENTICATED)
        try {
            fixture.personalization.updateBookmarkedShopIds(emptySet())
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected("shop"))
            runCurrent()
            block(fixture)
        } finally {
            fixture.store.clear()
        }
    }
