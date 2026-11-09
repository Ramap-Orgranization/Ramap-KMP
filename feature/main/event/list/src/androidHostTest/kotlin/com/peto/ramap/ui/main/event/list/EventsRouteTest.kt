package com.peto.ramap.ui.main.event.list

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.fake.FakeAnalyticsTracker
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
    fun `비회원 저장 필터를 선택해도 로그인 버튼을 표시하지 않는다`() =
        runComposeUiTest {
            setContent {
                val viewModel =
                    remember {
                        EventsViewModel(
                            FakeRamenShopRepository(),
                            EventsAnalytics(FakeAnalyticsTracker()),
                            FakePersonalizationRepository(),
                        )
                    }
                RamapTheme {
                    EventsRoute(
                        onClickEvent = {},
                        toastManager = ToastManager(),
                        viewModel = viewModel,
                    )
                }
            }
            onNodeWithContentDescription("저장한 매장").performTouchInput { click() }
            onNodeWithText("카카오로 시작하기").assertDoesNotExist()
        }
}
