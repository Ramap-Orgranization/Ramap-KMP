package com.peto.ramap.ui.onboarding

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.model.OnboardingGuidePage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingGuidePageContentTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `다음으로 두 번째 안내를 표시하고 예시 스위치는 실제 설정을 변경하지 않는다`() =
        runComposeUiTest {
            var page by mutableStateOf(OnboardingGuidePage.NotificationMenu)
            var skipCount = 0
            setContent {
                RamapTheme {
                    OnboardingGuideLayout(page = page) {
                        OnboardingGuidePageContent(
                            page = page,
                            onNext = { page = OnboardingGuidePage.NotificationTypes },
                            onSkip = { skipCount += 1 },
                        )
                    }
                }
            }
            onNodeWithText("매장 더보기에서 알림을 설정할 수 있어요.").assertIsDisplayed()
            onNodeWithText("건너뛰기").assertIsDisplayed()
            captureIfRequested(this, "notification-menu")
            onNodeWithText("다음").performClick()
            onNodeWithText("이벤트와 영업 변동 중\n받고 싶은 소식만 골라보세요.").assertIsDisplayed()
            onAllNodes(isToggleable()).assertCountEquals(0)
            captureIfRequested(this, "notification-types")
            onNodeWithText("건너뛰기").assertIsDisplayed().performClick()
            runOnIdle { assertEquals(1, skipCount) }
        }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `작은 화면에서도 모든 안내 단계의 다음과 건너뛰기가 보이고 눌린다`() =
        runComposeUiTest {
            var page by mutableStateOf(OnboardingGuidePage.NotificationMenu)
            var nextCount = 0
            var skipCount = 0
            setContent {
                RamapTheme {
                    OnboardingGuideLayout(page = page) {
                        OnboardingGuidePageContent(
                            page = page,
                            onNext = { nextCount += 1 },
                            onSkip = { skipCount += 1 },
                        )
                    }
                }
            }
            for (item in OnboardingGuidePage.entries) {
                runOnIdle { page = item }
                onNodeWithText("다음").assertIsDisplayed().performClick()
                onNodeWithText("건너뛰기").assertIsDisplayed().performClick()
                captureIfRequested(this, "${item.name.lowercase()}-compact")
            }
            runOnIdle { page = OnboardingGuidePage.NotificationTypes }
            onNodeWithContentDescription("이벤트, 임시휴무, 오픈 지연, 조기 마감, 단축 영업, 영업 일시 중단을 선택할 수 있는 매장 알림 예시 화면")
                .assertWidthIsAtLeast(240.dp)
            onNode(hasScrollAction()).performTouchInput { swipeUp() }
            onNodeWithText("다음").assertIsDisplayed()
            onNodeWithText("건너뛰기").assertIsDisplayed()
            captureIfRequested(this, "notification-types-compact-scrolled")
            runOnIdle {
                assertEquals(OnboardingGuidePage.entries.size, nextCount)
                assertEquals(OnboardingGuidePage.entries.size, skipCount)
            }
        }

    private fun captureIfRequested(
        scope: ComposeUiTest,
        name: String,
    ) {
        val directory = System.getenv("RAMAP_ONBOARDING_SCREENSHOTS_DIR") ?: return
        val file = File(directory, "$name.png")
        file.parentFile?.mkdirs()
        val bitmap =
            scope.runOnUiThread {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                val view = activity.findViewById<View>(android.R.id.content)
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
