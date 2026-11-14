package com.peto.ramap.ui.news

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class NewsRouteContentTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `저장 필터는 탭 줄 오른쪽에 표시하고 이벤트와 영업 변동에서 각각 적용한다`() =
        runComposeUiTest {
            val personalizationState = PersonalizationBootstrapState.Success(Personalization())
            setContent {
                RamapTheme {
                    val navigationState = rememberNavigationState()
                    var events by remember { mutableStateOf(EventsUiState(personalizationState = personalizationState)) }
                    var notices by remember { mutableStateOf(OperatingNoticeUiState(personalizationState = personalizationState)) }
                    NewsRouteContent(
                        route = ScreenRoutes.EventTabRoutes(),
                        eventsUiState = events,
                        noticeUiState = notices,
                        onClickEvent = {},
                        onShopClick = {},
                        onNewShopClick = {},
                        navigationState = navigationState,
                        onEventsIntent = { intent ->
                            if (intent == EventsIntent.OnSavedToggled) events = events.copy(savedOnly = !events.savedOnly)
                        },
                        onNoticeIntent = { intent ->
                            if (intent == OperatingNoticeIntent.OnSavedToggled) notices = notices.copy(savedOnly = !notices.savedOnly)
                        },
                    )
                }
            }

            val eventsTab = onNodeWithText("이벤트")
            val noticesTab = onNodeWithText("영업 변동")
            val savedFilter = onNodeWithContentDescription("저장한 매장")
            val searchInput = onNode(hasSetTextAction())

            searchInput.assertDoesNotExist()
            val eventsTabBounds = eventsTab.getBoundsInRoot()
            val filterBounds = savedFilter.getBoundsInRoot()
            assertTrue(filterBounds.left > eventsTabBounds.right)
            assertEquals(eventsTabBounds.verticalCenterInDp(), filterBounds.verticalCenterInDp(), absoluteTolerance = 1f)
            savedFilter.performClick().assertIsSelected()

            noticesTab.performClick().assertIsSelected()
            searchInput.assertDoesNotExist()
            savedFilter.assertIsNotSelected()
            val noticesTabBounds = noticesTab.getBoundsInRoot()
            val noticesFilterBounds = savedFilter.getBoundsInRoot()
            assertEquals(noticesTabBounds.verticalCenterInDp(), noticesFilterBounds.verticalCenterInDp(), absoluteTolerance = 1f)
            savedFilter.performClick().assertIsSelected()

            eventsTab.performClick().assertIsSelected()
            savedFilter.assertIsSelected()
        }

    private fun DpRect.verticalCenterInDp(): Float = (top.value + bottom.value) / 2f
}
