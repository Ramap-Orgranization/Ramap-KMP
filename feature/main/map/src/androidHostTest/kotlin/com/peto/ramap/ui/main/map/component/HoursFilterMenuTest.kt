package com.peto.ramap.ui.main.map.component

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDateTime
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HoursFilterMenuTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    @Config(qualifiers = "w360dp-h700dp")
    fun `영업시간 다이얼로그에서 방문 날짜와 시간을 고르고 적용한다`() =
        runComposeUiTest {
            var selected by mutableStateOf<ShopHoursFilter>(ShopHoursFilter.All)
            setContent {
                RamapTheme {
                    Box(Modifier.fillMaxSize()) {
                        HoursFilterMenu(
                            selected = selected,
                            now = LocalDateTime(2026, 11, 14, 18, 45),
                            resultCount = { filter ->
                                when (filter) {
                                    ShopHoursFilter.All -> 7
                                    ShopHoursFilter.OpenNow -> 2
                                    is ShopHoursFilter.VisitTime -> 3
                                }
                            },
                            onOpen = {},
                            onApply = { selected = it },
                        )
                    }
                }
            }

            onNodeWithText("영업시간").performClick()
            val dialogTitle = hasText("영업시간 필터 설정") and hasAnyAncestor(isDialog())
            waitUntilExactlyOneExists(dialogTitle)
            onNode(dialogTitle).assertIsDisplayed()
            onNodeWithText("전체 매장").assertIsSelected()
            onNodeWithText("필터 적용(7개)").assertIsDisplayed()
            onNodeWithText("2026년 11월").assertDoesNotExist()
            onNodeWithText("방문 시간 선택").performClick()
            onNodeWithText("2026년 11월").performScrollTo().assertIsDisplayed()
            onNodeWithContentDescription("11월 15일 일요일").performScrollTo().assertIsDisplayed().performClick()
            onNodeWithContentDescription("11월 15일 일요일").assertIsSelected()
            onNodeWithContentDescription("11월 14일 토요일").assertIsNotSelected()
            onNodeWithText("07:00").assertDoesNotExist()
            onNodeWithText("08:30").assertDoesNotExist()
            onNodeWithText("08:00").performScrollTo().performClick()
            val headerBounds = onNode(dialogTitle).getBoundsInRoot()
            val optionBounds = onNodeWithText("방문 시간 선택").getBoundsInRoot()
            val applyBounds = onNodeWithText("필터 적용(3개)").getBoundsInRoot()
            val month = onNodeWithText("2026년 11월")
            val monthBounds = month.getBoundsInRoot()
            onNodeWithText("23:00").performScrollTo().assertIsDisplayed()
            assertTrue(month.getBoundsInRoot() != monthBounds)
            assertEquals(headerBounds, onNode(dialogTitle).getBoundsInRoot())
            assertEquals(optionBounds, onNodeWithText("방문 시간 선택").getBoundsInRoot())
            assertEquals(applyBounds, onNodeWithText("필터 적용(3개)").getBoundsInRoot())

            onNodeWithText("영업 중").performClick().assertIsSelected()
            onNodeWithText("2026년 11월").assertDoesNotExist()
            onNodeWithText("08:00").assertDoesNotExist()
            onNodeWithText("필터 적용(2개)").assertIsDisplayed()
            onNodeWithText("방문 시간 선택").performClick()
            onNodeWithContentDescription("11월 15일 일요일").performScrollTo().assertIsSelected()
            onNodeWithText("08:00").performScrollTo().assertIsSelected()
            runOnIdle { assertEquals(ShopHoursFilter.All, selected) }

            onNode(hasText("필터 적용", substring = true)).assertIsDisplayed().performClick()
            runOnIdle { assertEquals(ShopHoursFilter.VisitTime(LocalDateTime(2026, 11, 15, 8, 0)), selected) }
            onNodeWithText("내일 08:00").assertIsDisplayed()

            onNodeWithText("내일 08:00").performClick()
            waitUntilExactlyOneExists(dialogTitle)
            onNodeWithText("초기화").performClick()
            onNodeWithText("전체 매장").assertIsSelected()
            onNodeWithContentDescription("닫기").performClick()
            runOnIdle { assertEquals(ShopHoursFilter.VisitTime(LocalDateTime(2026, 11, 15, 8, 0)), selected) }

            onNodeWithText("내일 08:00").performClick()
            waitUntilExactlyOneExists(dialogTitle)
            onNodeWithText("초기화").performClick()
            onNode(hasText("필터 적용", substring = true)).performClick()
            runOnIdle { assertEquals(ShopHoursFilter.All, selected) }
        }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun `작은 화면에서 마지막 시간까지 스크롤해도 제목과 하단 버튼을 사용할 수 있다`() =
        runComposeUiTest {
            var selected by mutableStateOf<ShopHoursFilter>(ShopHoursFilter.All)
            setContent {
                RamapTheme {
                    Box(Modifier.fillMaxSize()) {
                        HoursFilterMenu(
                            selected = selected,
                            now = LocalDateTime(2026, 11, 14, 6, 0),
                            resultCount = { 0 },
                            onOpen = {},
                            onApply = { selected = it },
                        )
                    }
                }
            }

            onNodeWithText("영업시간").performClick()
            val title = onNode(hasText("영업시간 필터 설정") and hasAnyAncestor(isDialog()))
            val apply = onNodeWithText("필터 적용(0개)")
            val compactHeight = apply.getBoundsInRoot().bottom - title.getBoundsInRoot().top

            onNodeWithText("방문 시간 선택").performClick()
            val expandedHeight = apply.getBoundsInRoot().bottom - title.getBoundsInRoot().top
            assertTrue(expandedHeight > compactHeight)
            val headerBounds = title.getBoundsInRoot()
            val applyBounds = apply.getBoundsInRoot()

            onNodeWithContentDescription("11월 20일 금요일").performScrollTo().assertIsDisplayed().performClick()
            onNodeWithText("23:00")
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
                .assertIsSelected()
            assertEquals(headerBounds, title.getBoundsInRoot())
            assertEquals(applyBounds, apply.getBoundsInRoot())
            onNodeWithText("초기화").assertIsDisplayed()
            apply.assertIsDisplayed().performClick()
            runOnIdle { assertEquals(ShopHoursFilter.VisitTime(LocalDateTime(2026, 11, 20, 23, 0)), selected) }
        }

    @Test
    fun `카테고리 줄을 가로로 밀면 영업시간 칩도 함께 스크롤된다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    MenuCategoryFilterRow(
                        selectedFilter = RamenShopFilter(),
                        now = LocalDateTime(2026, 11, 14, 18, 45),
                        hoursFilterResultCount = { 0 },
                        onHoursFilterOpened = {},
                        onHoursFilterApplied = {},
                        onCategoryClick = {},
                        modifier = Modifier.width(270.dp),
                    )
                }
            }

            val chip = onNodeWithText("영업시간").assertIsDisplayed()
            val before = chip.getBoundsInRoot()
            onRoot().performTouchInput { swipeLeft() }
            val after = chip.getBoundsInRoot()
            assertTrue(after.right < before.right)
            onRoot().performTouchInput { swipeRight() }
            chip.assertIsDisplayed().performClick()
            onNodeWithText("전체 매장").assertIsDisplayed()
        }
}
