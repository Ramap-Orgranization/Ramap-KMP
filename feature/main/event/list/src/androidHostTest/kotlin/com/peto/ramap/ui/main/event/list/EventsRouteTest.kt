package com.peto.ramap.ui.main.event.list

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class EventsRouteTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `저장 필터에서 연 로그인 안내의 버튼 터치가 로그인 호출로 전달된다`() =
        runComposeUiTest {
            val login = FakeLoginRepository()
            val tracker = FakeAnalyticsTracker()
            val toastManager = ToastManager()
            var underlyingClickCount = 0
            setContent {
                val dispatcher = remember { NavigationEventDispatcher() }
                DisposableEffect(dispatcher) { onDispose { dispatcher.dispose() } }
                val owner =
                    remember(dispatcher) {
                        object : NavigationEventDispatcherOwner {
                            override val navigationEventDispatcher = dispatcher
                        }
                    }
                val viewModel =
                    remember {
                        EventsViewModel(
                            FakeRamenShopRepository(),
                            EventsAnalytics(tracker),
                            FakePersonalizationRepository(),
                            login,
                            SignInUseCase(login, LoginAnalytics(tracker, FakeCrashReporter())),
                        )
                    }
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    RamapTheme {
                        EventsRoute(
                            onClickEvent = { underlyingClickCount += 1 },
                            onClickNotice = { underlyingClickCount += 1 },
                            toastManager = toastManager,
                            viewModel = viewModel,
                        )
                    }
                }
            }
            waitForIdle()
            onNodeWithContentDescription("저장한 매장").performTouchInput { click() }
            waitForIdle()
            mainClock.advanceTimeBy(500)
            onNodeWithText("카카오로 시작하기").assertExists().performTouchInput { click() }
            waitForIdle()
            runOnIdle {
                assertEquals(1, login.signInWithKakaoCallCount)
                assertEquals(0, underlyingClickCount)
            }
        }
}
