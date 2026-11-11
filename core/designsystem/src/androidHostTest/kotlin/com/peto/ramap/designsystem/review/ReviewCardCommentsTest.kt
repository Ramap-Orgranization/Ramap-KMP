package com.peto.ramap.designsystem.review

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
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
class ReviewCardCommentsTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `리뷰 사진과 본문은 상세를 열고 댓글은 펼치기만 한다`() =
        runComposeUiTest {
            var detailClicks = 0
            var likes = 0
            setContent {
                RamapTheme {
                    ReviewCard(
                        review =
                            Review(
                                "review",
                                "shop",
                                "리뷰 본문",
                                "2026-11-11T08:00:00Z",
                                imageUrls = listOf("android.resource://android/drawable/ic_menu_gallery"),
                                commentCount = 2,
                            ),
                        actions = ReviewCardActions(onOpenReviewDetail = { detailClicks++ }, onLike = { likes++ }),
                        commentsContent = {
                            AppText(text = "펼쳐진 댓글", style = AppTextStyle.B2, color = GrayColor.C500)
                        },
                        header = {},
                    )
                }
            }
            onNodeWithContentDescription("리뷰 사진 1").assertHasClickAction().performTouchInput { click() }
            onNode(isDialog()).assertDoesNotExist()
            runOnIdle { assertEquals(1, detailClicks) }
            onNodeWithText("펼쳐진 댓글", useUnmergedTree = true).assertDoesNotExist()
            onNodeWithContentDescription("댓글 2개", useUnmergedTree = true).performTouchInput { click() }
            onNodeWithText("펼쳐진 댓글", useUnmergedTree = true).assertIsDisplayed().performTouchInput { click() }
            onNodeWithContentDescription("댓글 2개", useUnmergedTree = true).performTouchInput { click() }
            onNodeWithText("펼쳐진 댓글", useUnmergedTree = true).assertDoesNotExist()
            runOnIdle {
                assertEquals(1, detailClicks)
                assertEquals(0, likes)
            }
            onNodeWithText("리뷰 본문").performTouchInput { click() }
            runOnIdle {
                assertEquals(2, detailClicks)
                assertEquals(0, likes)
            }
        }

    @Test
    fun `댓글 수가 0이면 아이콘을 눌러도 펼쳐지지 않고 펼친 뒤 0으로 바뀌면 접힌다`() =
        runComposeUiTest {
            val commentCount = mutableIntStateOf(0)
            setContent {
                RamapTheme {
                    ReviewCard(
                        review = Review("review", "shop", "리뷰 본문", "2026-11-11T08:00:00Z", commentCount = commentCount.intValue),
                        commentsContent = {
                            AppText(text = "펼쳐진 댓글", style = AppTextStyle.B2, color = GrayColor.C500)
                        },
                        header = {},
                    )
                }
            }

            onNodeWithContentDescription("댓글 0개").assertIsNotEnabled().performTouchInput { click() }
            onNodeWithText("펼쳐진 댓글").assertDoesNotExist()
            runOnIdle { commentCount.intValue = 2 }
            onNodeWithContentDescription("댓글 2개").performTouchInput { click() }
            onNodeWithText("펼쳐진 댓글").assertIsDisplayed()
            runOnIdle { commentCount.intValue = 0 }
            onNodeWithContentDescription("댓글 0개").assertIsNotEnabled()
            onNodeWithText("펼쳐진 댓글").assertDoesNotExist()
        }
}
