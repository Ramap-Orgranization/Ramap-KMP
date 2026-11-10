package com.peto.ramap

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.peto.ramap.domain.model.auth.SignupTerms
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.SignupConsentController
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
class SignupConsentHostTest {
    private val controller = SignupConsentController()
    private val terms = SignupTerms(version = "v1", url = "https://example.test/terms/v1")

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `약관을 읽는 것만으로 동의되지 않고 체크한 뒤에만 계속할 수 있다`() =
        runComposeUiTest {
            var accepted: Boolean? = null
            setContent {
                RamapTheme {
                    LaunchedEffect(Unit) { accepted = controller.requestConsent(terms) }
                    SignupConsentHost(controller)
                }
            }

            onNodeWithText("다음").assertIsNotEnabled()
            onNodeWithContentDescription("서비스 이용약관 보기").performClick()
            onNodeWithText("서비스 이용약관").assertIsDisplayed()
            onNodeWithText("목록으로 돌아가기").performClick()
            onNodeWithText("다음").assertIsNotEnabled()
            onNodeWithText("서비스 이용약관 동의").performClick()
            onNodeWithText("다음").assertIsEnabled()
            onNodeWithText("개인정보 처리방침").performClick()
            onNodeWithText("문서 전문 보기").assertIsDisplayed()
            onNodeWithText("목록으로 돌아가기").performClick()
            onNodeWithText("다음")
                .assertIsEnabled()
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            waitForIdle()
            assertEquals(true, accepted)
        }

    @Test
    fun `필수 약관을 체크해도 시트 바깥을 누르면 가입을 취소한다`() =
        runComposeUiTest {
            var accepted: Boolean? = null
            setContent {
                RamapTheme {
                    LaunchedEffect(Unit) { accepted = controller.requestConsent(terms) }
                    SignupConsentHost(controller)
                }
            }

            onNodeWithText("서비스 이용약관 동의").performClick()
            onRoot().performTouchInput { click(Offset(centerX, height * 0.15f)) }
            waitForIdle()
            assertEquals(false, accepted)
        }
}
