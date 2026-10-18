package com.peto.ramap.ui.paging

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
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
class InfiniteLazyColumnTest {
    private var pagination by mutableStateOf(InfiniteScrollState(itemCount = 1, hasMore = true))
    private var enabled by mutableStateOf(true)
    private var requestCount = 0

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `화면을 채우지 못하는 페이지를 이어서 요청하고 같은 개수와 마지막 페이지는 반복하지 않는다`() =
        runComposeUiTest {
            setContent { content() }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(isLoading = true)
            }
            runOnIdle { pagination = pagination.copy(isLoading = false) }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(itemCount = 2)
            }
            runOnIdle {
                assertEquals(2, requestCount)
                pagination = pagination.copy(itemCount = 3, hasMore = false)
            }
            runOnIdle { assertEquals(2, requestCount) }
        }

    @Test
    fun `데이터 키를 지정하면 헤더와 푸터를 제외하고 감지하며 목록 정체성 변경은 새 요청을 허용한다`() =
        runComposeUiTest {
            pagination = InfiniteScrollState(itemCount = 20, hasMore = true, itemKeys = List(20) { "item-$it" })
            setContent { content(withHeader = true) }
            runOnIdle { assertEquals(0, requestCount) }
            onNodeWithTag("infinite_list").performScrollToKey("item-19")
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(isLoading = true)
            }
            runOnIdle { pagination = pagination.copy(isLoading = false) }
            runOnIdle { assertEquals(1, requestCount) }
            onNodeWithTag("infinite_list").performScrollToKey("item-0")
            onNodeWithTag("infinite_list").performScrollToKey("item-19")
            runOnIdle {
                assertEquals(2, requestCount)
                pagination = pagination.copy(requestKey = "다른 목록")
            }
            runOnIdle { assertEquals(3, requestCount) }
        }

    @Test
    fun `비활성화와 새로고침 중에는 요청하지 않고 다시 활성화되면 요청한다`() =
        runComposeUiTest {
            enabled = false
            setContent { content() }
            runOnIdle {
                assertEquals(0, requestCount)
                enabled = true
            }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(isRefreshing = true)
            }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(isRefreshing = false)
            }
            runOnIdle { assertEquals(2, requestCount) }
        }

    @Test
    fun `기존 관찰자 호출은 데이터가 늘어도 끝 영역을 벗어났다가 돌아올 때 다시 요청한다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    val listState = rememberLazyListState()
                    ObserveLoadMoreNearListEnd(
                        listState = listState,
                        itemThreshold = 3,
                        hasMore = pagination.hasMore,
                        isLoading = pagination.isLoading,
                        onLoadMore = { requestCount++ },
                    )
                    LazyColumn(state = listState, modifier = Modifier.height(200.dp).width(300.dp).testTag("infinite_list")) {
                        dataItems(this)
                    }
                }
            }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(itemCount = 2)
            }
            runOnIdle {
                assertEquals(1, requestCount)
                pagination = pagination.copy(itemCount = 20)
            }
            onNodeWithTag("infinite_list").performScrollToKey("item-19")
            runOnIdle { assertEquals(2, requestCount) }
        }

    @Composable
    private fun content(withHeader: Boolean = false) {
        RamapTheme {
            InfiniteLazyColumn(
                loadMoreState = pagination,
                loadMoreEnabled = enabled,
                onLoadMore = { requestCount++ },
                modifier = Modifier.height(200.dp).width(300.dp).testTag("infinite_list"),
            ) {
                if (withHeader) item(key = "header") { Spacer(modifier = Modifier.height(400.dp)) }
                dataItems(this)
                if (pagination.isLoading) item(key = "loading") { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    private fun dataItems(scope: LazyListScope) {
        scope.items(count = pagination.itemCount, key = { "item-$it" }) { index ->
            AppText(
                text = "항목 $index",
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                modifier = Modifier.fillMaxWidth().height(40.dp),
            )
        }
    }
}
