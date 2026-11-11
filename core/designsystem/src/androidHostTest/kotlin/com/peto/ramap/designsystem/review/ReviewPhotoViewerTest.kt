package com.peto.ramap.designsystem.review

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.peto.ramap.theme.RamapTheme
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewPhotoViewerTest {
    private val inViewer = hasAnyAncestor(isDialog())
    private val imageUrls =
        listOf(
            "android.resource://android/drawable/ic_menu_gallery",
            "android.resource://android/drawable/ic_menu_camera",
            "android.resource://android/drawable/ic_menu_report_image",
        )

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `사진 뷰어는 시스템바 뒤까지 그리며 밝은 시스템바 아이콘을 사용한다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    ReviewCardImages(imageUrls = imageUrls.take(1))
                }
            }

            onNodeWithContentDescription("리뷰 사진 1").performClick()
            runOnIdle {
                val window = requireNotNull(ShadowDialog.getLatestDialog().window)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                assertEquals(0, window.attributes.fitInsetsTypes)
                assertFalse(window.isNavigationBarContrastEnforced)
                assertFalse(controller.isAppearanceLightStatusBars)
                assertFalse(controller.isAppearanceLightNavigationBars)
            }
        }

    @Test
    fun `누른 사진으로 열리고 스와이프와 썸네일 선택에 따라 큰 사진과 번호가 함께 바뀐다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    ReviewCardImages(imageUrls = imageUrls, imageSize = 72.dp)
                }
            }

            onNodeWithContentDescription("리뷰 사진 2").performClick()
            onNodeWithText("2 / 3").assertIsDisplayed()
            onNode(inViewer and hasClickAction() and hasContentDescription("리뷰 사진 2")).assertIsSelected()
            onNode(inViewer and !hasClickAction() and hasContentDescription("리뷰 사진 2"))
                .assertIsDisplayed()
                .performTouchInput { swipeLeft() }

            onNodeWithText("3 / 3").assertIsDisplayed()
            onNode(inViewer and hasClickAction() and hasContentDescription("리뷰 사진 3")).assertIsSelected()
            onNode(inViewer and !hasClickAction() and hasContentDescription("리뷰 사진 3")).assertIsDisplayed()

            onNode(inViewer and hasClickAction() and hasContentDescription("리뷰 사진 1")).performClick().assertIsSelected()
            onNodeWithText("1 / 3").assertIsDisplayed()
            onNode(inViewer and !hasClickAction() and hasContentDescription("리뷰 사진 1")).assertIsDisplayed()

            onNodeWithContentDescription("닫기").performClick()
            onNodeWithContentDescription("리뷰 사진 3").performClick()
            onNodeWithText("3 / 3").assertIsDisplayed()
        }

    @Test
    fun `사진이 한 장이면 큰 사진만 표시하고 썸네일과 번호는 숨긴다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    ReviewCardImages(imageUrls = imageUrls.take(1))
                }
            }

            onNodeWithContentDescription("리뷰 사진 1").performClick()
            onNode(inViewer and !hasClickAction() and hasContentDescription("리뷰 사진 1")).assertIsDisplayed()
            onNode(inViewer and hasClickAction() and hasContentDescription("리뷰 사진 1")).assertDoesNotExist()
            onNodeWithText("1 / 1").assertDoesNotExist()
        }
}
