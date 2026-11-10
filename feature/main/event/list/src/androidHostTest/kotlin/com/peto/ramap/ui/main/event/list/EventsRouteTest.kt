package com.peto.ramap.ui.main.event.list

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
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
    fun `비회원 저장 필터에 해당하는 이벤트가 없으면 빈 안내를 표시한다`() =
        runComposeUiTest {
            setContent { Content() }
            onNodeWithContentDescription("저장한 매장").performTouchInput { click() }
            onNodeWithText("저장한 매장에 이벤트가 없어요.").assertIsDisplayed()
            onNodeWithText("카카오로 시작하기").assertDoesNotExist()
        }

    @Test
    fun `빈 이벤트 목록에서 매장명을 검색하면 검색 결과가 없음을 안내한다`() =
        runComposeUiTest {
            setContent { Content() }
            onNodeWithText("진행 중이거나 예정된 이벤트가 없어요").assertIsDisplayed()

            onNodeWithText("매장명을 입력해주세요").performTextInput("없는 매장")
            onNodeWithText("조건에 맞는 이벤트가 없어요.").assertIsDisplayed()
        }

    @Composable
    private fun Content() {
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
}
