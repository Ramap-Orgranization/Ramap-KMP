package com.peto.ramap

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.login.rememberLoginGuideRequest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.kakao_login_button_title
import ramap.shared.generated.resources.login_guide_close
import ramap.shared.generated.resources.login_required_bookmark_description
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_message
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class LoginGuideHostTest {
    private val controller = LoginGuideController()

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `공통 Host의 로그인 버튼은 재구성된 화면의 최신 콜백을 호출한다`() =
        runComposeUiTest {
            var callbackVersion by mutableStateOf(1)
            val selected = mutableListOf<Pair<Int, LoginType>>()
            var dismissed = 0
            lateinit var requestLoginGuide: (StringResource) -> Unit
            var buttonTitle = ""
            var description = ""
            var previousDescription = ""
            var previousTitle = ""
            setContent {
                val version = callbackVersion
                requestLoginGuide =
                    rememberLoginGuideRequest(
                        onLoginTypeSelected = { selected += version to it },
                        onDismiss = { dismissed++ },
                        controller = controller,
                    )
                buttonTitle = stringResource(Res.string.kakao_login_button_title)
                description = stringResource(Res.string.login_required_description)
                previousDescription = stringResource(Res.string.login_required_bookmark_description)
                previousTitle = stringResource(Res.string.login_required_message)
                RamapTheme {
                    Box(Modifier.fillMaxSize()) { LoginGuideHost(controller) }
                }
            }
            runOnIdle { requestLoginGuide(Res.string.login_required_bookmark_description) }
            onNodeWithText(description).assertIsDisplayed()
            onNodeWithText(previousDescription).assertDoesNotExist()
            onNodeWithText(previousTitle).assertDoesNotExist()
            runOnIdle { callbackVersion = 2 }

            onNodeWithText(buttonTitle).performClick()

            runOnIdle {
                assertEquals(listOf(2 to LoginType.KAKAO), selected)
                assertEquals(0, dismissed)
                assertNull(controller.request.value)
            }
            onNodeWithText(description).assertDoesNotExist()
        }

    @Test
    fun `요청 화면이 사라지면 루트 안내와 대기 동작을 함께 정리한다`() =
        runComposeUiTest {
            var showScreen by mutableStateOf(true)
            var dismissed = 0
            val selected = mutableListOf<LoginType>()
            lateinit var requestLoginGuide: (StringResource) -> Unit
            var description = ""
            setContent {
                if (showScreen) {
                    requestLoginGuide =
                        rememberLoginGuideRequest(
                            onLoginTypeSelected = { selected += it },
                            onDismiss = { dismissed++ },
                            controller = controller,
                        )
                }
                description = stringResource(Res.string.login_required_description)
                RamapTheme {
                    Box(Modifier.fillMaxSize()) { LoginGuideHost(controller) }
                }
            }
            runOnIdle { requestLoginGuide(Res.string.login_required_bookmark_description) }
            onNodeWithText(description).assertIsDisplayed()
            val request = runOnIdle { assertNotNull(controller.request.value) }

            runOnIdle { showScreen = false }

            onNodeWithText(description).assertDoesNotExist()
            runOnIdle {
                controller.select(request, LoginType.KAKAO)
                assertNull(controller.request.value)
                assertEquals(1, dismissed)
                assertEquals(emptyList(), selected)
            }
        }

    @Test
    fun `닫기 버튼은 로그인 실행 없이 요청과 대기 동작을 한 번 정리한다`() =
        runComposeUiTest {
            var dismissed = 0
            val selected = mutableListOf<LoginType>()
            var closeDescription = ""
            setContent {
                closeDescription = stringResource(Res.string.login_guide_close)
                RamapTheme {
                    Box(Modifier.fillMaxSize()) { LoginGuideHost(controller) }
                }
            }
            runOnIdle {
                controller.show(
                    owner = Any(),
                    description = Res.string.login_required_bookmark_description,
                    onLoginTypeSelected = { selected += it },
                    onDismiss = { dismissed++ },
                )
            }
            val request = runOnIdle { assertNotNull(controller.request.value) }

            onNodeWithContentDescription(closeDescription).performClick()

            runOnIdle {
                controller.dismiss(request)
                controller.select(request, LoginType.KAKAO)
                assertNull(controller.request.value)
                assertEquals(1, dismissed)
                assertEquals(emptyList(), selected)
            }
            onNodeWithContentDescription(closeDescription).assertDoesNotExist()
        }

    @Test
    fun `화면 대상이 바뀌면 기존 수집자가 가진 요청 함수도 새 화면에 연결한다`() =
        runComposeUiTest {
            var screenKey by mutableStateOf("first")
            val dismissed = mutableListOf<String>()
            val selected = mutableListOf<String>()
            lateinit var requestLoginGuide: (StringResource) -> Unit
            setContent {
                val screen = screenKey
                requestLoginGuide =
                    rememberLoginGuideRequest(
                        onLoginTypeSelected = { selected += screen },
                        onDismiss = { dismissed += screen },
                        ownerKey = screen,
                        controller = controller,
                    )
            }
            val originalRequestLoginGuide = runOnIdle { requestLoginGuide }
            runOnIdle {
                originalRequestLoginGuide(Res.string.login_required_bookmark_description)
                screenKey = "second"
            }
            runOnIdle {
                assertEquals(listOf("first"), dismissed)
                assertNull(controller.request.value)
                originalRequestLoginGuide(Res.string.login_required_bookmark_description)
                controller.select(assertNotNull(controller.request.value), LoginType.KAKAO)
                assertEquals(listOf("second"), selected)
            }
        }

    @Test
    fun `뒤로 가기는 로그인 안내를 먼저 닫고 아래 화면은 유지한다`() =
        runComposeUiTest {
            var screenBackCount = 0
            var dismissed = 0
            lateinit var dispatcher: OnBackPressedDispatcher
            lateinit var requestLoginGuide: (StringResource) -> Unit
            setContent {
                dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                requestLoginGuide =
                    rememberLoginGuideRequest(
                        onLoginTypeSelected = {},
                        onDismiss = { dismissed++ },
                        controller = controller,
                    )
                val backState = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None)
                NavigationBackHandler(
                    state = backState,
                    isBackEnabled = true,
                    onBackCompleted = { screenBackCount++ },
                )
                RamapTheme {
                    Box(Modifier.fillMaxSize()) { LoginGuideHost(controller) }
                }
            }
            runOnIdle { requestLoginGuide(Res.string.login_required_bookmark_description) }
            waitForIdle()

            runOnIdle { dispatcher.onBackPressed() }

            runOnIdle {
                assertNull(controller.request.value)
                assertEquals(1, dismissed)
                assertEquals(0, screenBackCount)
                dispatcher.onBackPressed()
                assertEquals(1, screenBackCount)
            }
        }
}
