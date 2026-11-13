package com.peto.ramap.ui.review.write.component

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.review.ReviewAttachment
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
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
class ReviewPhotoEditorTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `텍스트가 없는 세 번째 빈 칸을 눌러도 사진 선택을 연다`() =
        runComposeUiTest {
            var photoPickerOpens = 0
            setContent {
                RamapTheme {
                    PhotoEditor(ReviewWriteUiState(), onPick = { photoPickerOpens++ }, dispatch = {})
                }
            }

            onAllNodesWithContentDescription("사진 추가")[1].performTouchInput { click() }

            runOnIdle { assertEquals(1, photoPickerOpens) }
        }

    @Test
    fun `기존 사진을 오른쪽 끝에 끌면 스크롤해서 화면 밖 마지막 사진 위치로 옮긴다`() =
        runComposeUiTest {
            val moves = mutableListOf<ReviewWriteIntent>()
            val state = ReviewWriteUiState(attachments = List(5) { ReviewAttachment.Existing("photo-$it", null) })
            setContent {
                RamapTheme {
                    Box(Modifier.width(320.dp)) {
                        PhotoEditor(state, onPick = {}, dispatch = moves::add)
                    }
                }
            }

            val firstPhoto = onNodeWithContentDescription("리뷰 사진 1")
            mainClock.autoAdvance = false
            firstPhoto.performTouchInput {
                down(center)
                advanceEventTime(700)
                moveTo(Offset(width * 2.75f, centerY))
            }
            mainClock.advanceTimeBy(1_200)
            onRoot().performTouchInput { up() }
            mainClock.autoAdvance = true

            runOnIdle { assertEquals(listOf<ReviewWriteIntent>(ReviewWriteIntent.MoveImage(0, 4)), moves) }
        }
}
