package com.peto.ramap.ui.shop

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.fixture.ramenShopFixture
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
class ShopSummaryAccessibilityTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `매장 정보에 클릭 동작이 없으면 접근성 버튼으로 노출하지 않는다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    RamenShopSummary(
                        shop = ramenShopFixture(id = "shop", name = "테스트 매장"),
                    )
                }
            }

            onNodeWithText("테스트 매장").assert(hasClickAction().not())
        }

    @Test
    fun `매장 정보에 클릭 동작이 있으면 선택할 수 있다`() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                RamapTheme {
                    RamenShopSummary(
                        shop = ramenShopFixture(id = "shop", name = "테스트 매장"),
                        onClick = { clicks++ },
                    )
                }
            }

            onNodeWithText("테스트 매장").assert(hasClickAction()).performClick()
            runOnIdle { assertEquals(1, clicks) }
        }
}
