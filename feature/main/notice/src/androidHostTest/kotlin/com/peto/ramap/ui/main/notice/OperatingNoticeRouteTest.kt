package com.peto.ramap.ui.main.notice

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import kotlinx.datetime.LocalDate
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
class OperatingNoticeRouteTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `알림 공지를 로드한 뒤 시트를 열고 닫은 후 새로고침해도 다시 열지 않는다`() =
        runComposeUiTest {
            val date = LocalDate(2026, 11, 9)
            val notice =
                OperatingNotice(
                    id = "target",
                    shop =
                        ramenShopFixture(),
                    type = OperatingNoticeType.TEMPORARY_CLOSURE,
                    description = "알림 대상 공지",
                    startDate = date,
                    endDate = date,
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                )
            val state =
                mutableStateOf(
                    OperatingNoticeUiState(
                        savedOnly = true,
                        personalizationState = PersonalizationBootstrapState.Success(Personalization()),
                        fallbackReferenceDate = date,
                    ),
                )
            val pending = mutableStateOf<String?>(notice.id)
            var opens = 0
            setContent {
                val dispatcher = remember { NavigationEventDispatcher() }
                DisposableEffect(dispatcher) { onDispose { dispatcher.dispose() } }
                val owner =
                    remember(dispatcher) {
                        object : NavigationEventDispatcherOwner {
                            override val navigationEventDispatcher = dispatcher
                        }
                    }
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    RamapTheme {
                        OperatingNoticeScreen(
                            uiState = state.value,
                            onRefresh = {},
                            onRetry = {},
                            onShopClick = {},
                            onNoticeClick = { opens += 1 },
                            isSourceUrlSupported = { true },
                            onSourceClick = {},
                            initialNoticeId = pending.value,
                            onInitialNoticeOpened = { pending.value = null },
                        )
                    }
                }
            }
            waitForIdle()
            onNodeWithText("알림 대상 공지").assertDoesNotExist()
            runOnIdle { state.value = state.value.copy(todayOperatingNotices = listOf(notice)) }
            waitForIdle()
            mainClock.advanceTimeBy(500)
            onNodeWithText("알림 대상 공지").assertExists()
            onRoot().performTouchInput {
                click(
                    Offset(1f, 1f),
                )
            }
            waitForIdle()
            onNodeWithText("알림 대상 공지").assertDoesNotExist()
            runOnIdle {
                state.value =
                    state.value.copy(todayOperatingNotices = listOf(notice.copy(sourceUrl = "https://example.com/notice")))
            }
            waitForIdle()
            onNodeWithText("알림 대상 공지").assertDoesNotExist()
            runOnIdle { assertEquals(1, opens) }
        }
}
