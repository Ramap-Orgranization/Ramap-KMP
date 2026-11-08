package com.peto.ramap.ui.shop.overview

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.navigation.deeplink.ShopLinkConfig
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import kotlinx.coroutines.CompletableDeferred
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class OverviewNotificationPermissionTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `이미 허용된 권한은 안내와 권한 요청 없이 선택한 항목을 저장한다`() = permissionScenario(stopWhileWaiting = false, permissionGranted = true, initiallyGranted = true)

    @Test
    fun `항목을 켜서 권한을 요청한 뒤 화면이 중지되어도 복귀 후 저장할 수 있다`() = permissionScenario(stopWhileWaiting = true, permissionGranted = true)

    @Test
    fun `권한을 거절하면 안내를 보여주고 선택한 항목은 저장할 수 있다`() = permissionScenario(stopWhileWaiting = true, permissionGranted = false)

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `작은 화면에서도 안내에서 설정을 열 수 있고 허용 후 복귀하면 안내가 사라진다`() = permissionScenario(stopWhileWaiting = false, permissionGranted = false, enableInSettings = true)

    @Test
    fun `저장 실패 토스트를 다이얼로그 위에 표시하고 선택한 항목으로 다시 저장할 수 있다`() = permissionScenario(stopWhileWaiting = false, permissionGranted = true, initiallyGranted = true, failFirstSave = true)

    private fun permissionScenario(
        stopWhileWaiting: Boolean,
        permissionGranted: Boolean,
        initiallyGranted: Boolean = false,
        enableInSettings: Boolean = false,
        failFirstSave: Boolean = false,
    ) = runComposeUiTest {
        lateinit var fixture: OverviewViewModelFixture
        lateinit var lifecycleRegistry: LifecycleRegistry
        val permissionResult = CompletableDeferred<Boolean>()
        var permissionRequests = 0
        var currentPermissionGranted = initiallyGranted
        var settingsOpenCount = 0
        val guideMessage = "매장 알림을 받기 위해 알림 권한을 허용해주세요"
        val owner =
            object : LifecycleOwner {
                override val lifecycle: Lifecycle get() = lifecycleRegistry
            }
        runOnIdle {
            lifecycleRegistry = LifecycleRegistry.createUnsafe(owner).apply { currentState = Lifecycle.State.RESUMED }
            fixture = OverviewViewModelFixture(LoginSessionState.AUTHENTICATED)
            fixture.personalization.updateBookmarkedShopIds(emptySet())
            if (failFirstSave) {
                fixture.saveNotificationSettings = { settings, selection ->
                    if (fixture.notificationSaveRequests.size == 1) {
                        RamapResult.Error(RamapError.Http(500))
                    } else {
                        RamapResult.Success(settings.copy(selection = selection))
                    }
                }
            }
        }
        try {
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(module { single { LoginGuideController() } }) }) {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        RamapTheme {
                            OverviewRoute(
                                shopId = "shop",
                                onDismissRequest = {},
                                onShopViewed = {},
                                onShopLoadFailed = {},
                                onLoginGuideRequested = {},
                                toastManager = remember { ToastManager() },
                                appSettingsOpener =
                                    object : AppSettingsOpener {
                                        override fun open() {
                                            settingsOpenCount++
                                        }
                                    },
                                shopShareLinkFactory = remember { ShopShareLinkFactory(ShopLinkConfig("https://ramap.test", "ramap.test")) },
                                requestNotificationPermission = {
                                    permissionRequests++
                                    val granted = if (stopWhileWaiting) permissionResult.await() else permissionGranted
                                    currentPermissionGranted = granted
                                    granted
                                },
                                isNotificationPermissionGranted = { currentPermissionGranted },
                                viewModel = fixture.viewModel,
                            ) { _, _ -> }
                        }
                    }
                }
            }
            waitForIdle()
            runOnIdle { fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(ramenShopFixture(id = "shop"))) }
            waitForIdle()
            runOnIdle {
                assertNotNull(fixture.viewModel.uiState.value.notificationSettings)
                assertEquals(initiallyGranted, fixture.viewModel.uiState.value.isNotificationPermissionGranted)
                assertEquals(0, permissionRequests)
                fixture.viewModel.dispatch(ShopOverviewIntent.OnNotificationSelectAll)
            }
            waitForIdle()
            if (stopWhileWaiting) {
                runOnIdle {
                    assertNotNull(fixture.viewModel.uiState.value.notificationPermissionGeneration)
                    assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }
                waitForIdle()
                runOnIdle {
                    permissionResult.complete(permissionGranted)
                    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                }
                waitForIdle()
            }
            runOnIdle {
                assertEquals(if (initiallyGranted) 0 else 1, permissionRequests)
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                assertNull(fixture.viewModel.uiState.value.notificationPermissionGeneration)
                assertTrue(fixture.notificationSaveRequests.isEmpty())
                assertEquals(fixture.notificationCatalog.selectAll(), fixture.viewModel.uiState.value.notificationDraft)
            }
            if (currentPermissionGranted) {
                onNodeWithText(guideMessage).assertDoesNotExist()
            } else {
                onNodeWithText(guideMessage).performScrollTo().assertExists()
                onNodeWithText("저장").performScrollTo().assertIsEnabled()
                val guideBounds = onNodeWithText(guideMessage).fetchSemanticsNode().boundsInRoot
                val saveBounds = onNodeWithText("저장").fetchSemanticsNode().boundsInRoot
                assertTrue(guideBounds.bottom <= saveBounds.top)
            }
            if (enableInSettings) {
                onNodeWithText(guideMessage).performScrollTo().performClick()
                runOnIdle {
                    assertEquals(1, settingsOpenCount)
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }
                waitForIdle()
                runOnIdle {
                    currentPermissionGranted = true
                    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                }
                waitForIdle()
                onNodeWithText(guideMessage).assertDoesNotExist()
                runOnIdle {
                    assertEquals(1, permissionRequests)
                    assertEquals("shop", fixture.viewModel.uiState.value.notificationSettingsShopId)
                    assertEquals(fixture.notificationCatalog.selectAll(), fixture.viewModel.uiState.value.notificationDraft)
                    assertTrue(fixture.notificationSaveRequests.isEmpty())
                }
            }
            onNodeWithText("저장").performScrollTo().performClick()
            waitForIdle()
            if (failFirstSave) {
                val failureMessage = "알림 설정을 저장하지 못했어요. 다시 시도해주세요"
                onNodeWithText(failureMessage).assertIsDisplayed().assert(hasAnyAncestor(isDialog()))
                onNodeWithText("저장").assertIsEnabled()
                runOnIdle {
                    assertEquals("shop", fixture.viewModel.uiState.value.notificationSettingsShopId)
                    assertEquals(fixture.notificationCatalog.selectAll(), fixture.viewModel.uiState.value.notificationDraft)
                    assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
                }
                mainClock.advanceTimeBy(2_500)
                waitForIdle()
                onNodeWithText(failureMessage).assertDoesNotExist()
                onNodeWithText("저장").performClick()
                waitForIdle()
            }
            runOnIdle {
                assertEquals(if (initiallyGranted) 0 else 1, permissionRequests)
                assertEquals(List(if (failFirstSave) 2 else 1) { fixture.notificationCatalog.selectAll() }, fixture.notificationSaveRequests)
                assertNull(fixture.viewModel.uiState.value.notificationSettingsShopId)
                assertFalse(fixture.viewModel.uiState.value.isNotificationSettingsSaving)
            }
        } finally {
            runOnIdle { fixture.store.clear() }
        }
    }
}
