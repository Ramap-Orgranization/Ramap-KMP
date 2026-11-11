package com.peto.ramap.designsystem.profile

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
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
class ProfileVisitRankingCardTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `집계된 매장이 세 곳 미만이면 카드를 숨기고 세 곳이면 시상대만 표시한다`() =
        runComposeUiTest {
            val shopCount = mutableIntStateOf(0)
            setContent {
                RamapTheme {
                    ProfileVisitRankingCard(
                        ranking = visitRanking(shopCount.intValue),
                        onShopClick = {},
                    )
                }
            }

            onNodeWithText("방문 통계").assertDoesNotExist()
            for (count in 1..2) {
                runOnIdle { shopCount.intValue = count }
                onNodeWithText("방문 통계").assertDoesNotExist()
            }

            runOnIdle { shopCount.intValue = 3 }
            onNodeWithText("방문 통계").assertIsDisplayed()
            for (rank in 1..3) onNodeWithText("${rank}위").assertIsDisplayed()
            onNodeWithText("더보기").assertDoesNotExist()
        }

    @Test
    fun `시상대 순위에 동률이 있으면 카드를 숨기고 하위 동률은 순위 대신 대시로 표시한다`() =
        runComposeUiTest {
            val ranking = mutableStateOf(visitRanking(9L, 9L, 7L, 6L))
            setContent {
                RamapTheme {
                    ProfileVisitRankingCard(
                        ranking = ranking.value,
                        onShopClick = {},
                    )
                }
            }

            onNodeWithText("방문 통계").assertDoesNotExist()
            runOnIdle { ranking.value = visitRanking(9L, 7L, 7L, 6L) }
            onNodeWithText("방문 통계").assertDoesNotExist()
            runOnIdle { ranking.value = visitRanking(9L, 8L, 7L, 7L) }
            onNodeWithText("방문 통계").assertDoesNotExist()

            runOnIdle { ranking.value = visitRanking(9L, 8L, 7L, 6L, 6L, 5L) }
            onNodeWithText("3위").assertIsDisplayed()
            onNodeWithText("더보기").performClick()
            onAllNodesWithText("-").assertCountEquals(2)
            onNodeWithText("4위").assertDoesNotExist()
            onNodeWithText("5위").assertDoesNotExist()
            onNodeWithText("6위").assertIsDisplayed()
        }

    @Test
    fun `시상대와 더보기 목록의 순위나 배지를 눌러 매장을 열 수 있다`() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                RamapTheme {
                    ProfileVisitRankingCard(
                        ranking = visitRanking(10),
                        onShopClick = opened::add,
                    )
                }
            }

            onNodeWithText("1위").performClick()
            onNodeWithText("4위").assertDoesNotExist()
            onNodeWithText("더보기").performClick()
            onAllNodesWithText("1위").assertCountEquals(1)
            onNodeWithText("4위").assertIsDisplayed()
            onNodeWithText("10위").assertIsDisplayed()
            onNodeWithText("7번").assertIsDisplayed()
            onNodeWithText("4위").performClick()
            onNodeWithText("7번").performClick()
            onNodeWithText("10위").performClick()
            runOnIdle { assertEquals(listOf("1", "4", "4", "10"), opened) }
            onNodeWithText("접기").performClick()
            onNodeWithText("4위").assertDoesNotExist()
        }

    private fun visitRanking(shopCount: Int): ProfileVisitRanking = visitRanking(*(1..shopCount).map { 11L - it }.toLongArray())

    private fun visitRanking(vararg visitCounts: Long): ProfileVisitRanking =
        ProfileVisitRanking(
            values =
                visitCounts.mapIndexed { index, visitCount ->
                    val rank = index + 1
                    ProfileVisitedShop(rank.toString(), "매장 $rank", visitCount)
                },
        )
}
