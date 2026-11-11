package com.peto.ramap.designsystem.profile

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun `My의 안내 버튼은 내가 작성한 리뷰의 집계 기준을 열고 닫는다`() =
        verifyGuide(
            publicReviewsOnly = false,
            expectedGuide = "공개 여부와 관계없이 내가 작성한 리뷰를 기준으로 집계해요",
        )

    @Test
    fun `타인 프로필의 안내 버튼은 볼 수 있는 공개 리뷰의 집계 기준을 열고 닫는다`() =
        verifyGuide(
            publicReviewsOnly = true,
            expectedGuide = "볼 수 있는 공개 리뷰를 기준으로 집계해요",
        )

    private fun verifyGuide(
        publicReviewsOnly: Boolean,
        expectedGuide: String,
    ) = runComposeUiTest {
        setContent {
            RamapTheme {
                ProfileVisitRankingCard(
                    ranking = ProfileVisitRanking(listOf(ProfileVisitedShop("shop", "라멘집", 2))),
                    onShopClick = {},
                    loading = false,
                    failed = false,
                    publicReviewsOnly = publicReviewsOnly,
                    onRetry = {},
                )
            }
        }

        val toggle = onNodeWithContentDescription("방문 통계 집계 기준")
        onNodeWithText(expectedGuide).assertDoesNotExist()
        toggle.performClick()
        onNodeWithText(expectedGuide).assertIsDisplayed()
        toggle.performClick()
        onNodeWithText(expectedGuide).assertDoesNotExist()
    }
}
