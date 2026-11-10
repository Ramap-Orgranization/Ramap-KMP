package com.peto.ramap.designsystem.component

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.RamapTheme
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSearchBarTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `검색 모드가 바뀌어도 입력 시작점이 유지되고 지우기와 완료가 동작한다`() =
        runComposeUiTest {
            val submittedQueries = mutableListOf<String>()
            setContent {
                RamapTheme {
                    var query by remember { mutableStateOf("") }
                    var isSearchMode by remember { mutableStateOf(false) }

                    AppSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onFocusChanged = { isSearchMode = it },
                        isSearchMode = isSearchMode,
                        onSearch = { submittedQueries.add(query) },
                    )
                }
            }

            val input = onNode(hasSetTextAction())
            val placeholder = onNodeWithText("매장명을 입력해주세요", useUnmergedTree = true)
            val searchIcon = onNodeWithContentDescription("검색", useUnmergedTree = true)
            val inputStart = placeholder.getUnclippedBoundsInRoot().left
            assertEquals(4.dp, inputStart - searchIcon.getUnclippedBoundsInRoot().right, "아이콘과 입력칸 사이 간격")

            searchIcon.performTouchInput { click() }
            input.assertIsFocused()
            assertEquals(inputStart, placeholder.getUnclippedBoundsInRoot().left, "검색 모드 진입 후 입력 시작점")

            input.performTextInput("라멘")
            onNodeWithContentDescription("검색어 지우기").performClick()
            input.assertIsFocused()
            assertEquals(inputStart, placeholder.getUnclippedBoundsInRoot().left, "검색어를 지운 뒤 입력 시작점")

            input.performTextInput("미소")
            input.performImeAction()
            input.assertIsNotFocused()
            runOnIdle { assertEquals(listOf("미소"), submittedQueries, "키보드 완료로 제출한 검색어") }

            input.performClick()
            onNodeWithContentDescription("검색").performClick()
            input.assertIsNotFocused()
        }
}
