package com.peto.ramap.ui.main.notice

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.notice.OperatingNoticeType
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

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class OperatingNoticeScreenTest {
    private val today = LocalDate(2026, 10, 22)
    private var state by mutableStateOf(OperatingNoticeUiState())

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `오늘과 예정 섹션의 매장을 함께 표시하고 지난 공지는 제외한다`() =
        runComposeUiTest {
            state =
                state(
                    notice("오늘 매장", today),
                    notice("예정 매장", LocalDate(2026, 10, 23)),
                    notice("지난 매장", LocalDate(2026, 10, 21)),
                )
            setContent { content() }

            onNodeWithText("오늘의 운영 변동").assertExists()
            onNodeWithText("예정된 운영 변동").assertExists()
            onNodeWithText("오늘 매장").assertExists()
            onNodeWithText("예정 매장").assertExists()
            onNodeWithText("지난 매장").assertDoesNotExist()
            onNodeWithText("오늘(1)").assertDoesNotExist()
            onNodeWithText("10.23", substring = true).assertDoesNotExist()
        }

    @Test
    fun `전체 공지가 없으면 기존 빈 안내를 표시한다`() =
        runComposeUiTest {
            state = state()
            setContent { content() }
            onNodeWithText("현재 또는 예정된 영업 변동이 없어요").assertExists()
        }

    @Test
    fun `검색어를 입력하면 바로 필터링하고 지우면 전체 매장을 다시 표시한다`() =
        runComposeUiTest {
            state = state(notice("오늘 매장", today), notice("다른 매장", today))
            setContent { content() }

            onNodeWithText("매장명을 입력해주세요").performTextInput("오늘")
            onNodeWithText("오늘 매장").assertExists()
            onNodeWithText("다른 매장").assertDoesNotExist()

            onNodeWithContentDescription("검색어 지우기").performClick()
            onNodeWithText("매장명을 입력해주세요").assertExists()
            onNodeWithText("오늘 매장").assertExists()
            onNodeWithText("다른 매장").assertExists()
        }

    @Composable
    private fun content() {
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
                    uiState = state,
                    onBack = {},
                    onEventListClick = {},
                    onRefresh = {},
                    onRetry = {},
                    onShopClick = {},
                    onNoticeClick = {},
                    onQueryChanged = { state = state.copy(query = it) },
                    isSourceUrlSupported = { true },
                    onSourceClick = {},
                )
            }
        }
    }

    private fun state(vararg notices: OperatingNotice) =
        OperatingNoticeUiState(
            feed = OperatingNoticeFeed(today, notices.map { OperatingNoticeFeedItem(it) }),
        )

    private fun notice(
        name: String,
        date: LocalDate,
    ) = OperatingNotice(
        id = name,
        shop = ramenShopFixture(id = name, name = name),
        type = OperatingNoticeType.TEMPORARY_CLOSURE,
        description = "운영 일정",
        startDate = date,
        endDate = date,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )
}
