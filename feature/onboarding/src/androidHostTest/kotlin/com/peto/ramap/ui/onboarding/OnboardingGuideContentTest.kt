package com.peto.ramap.ui.onboarding

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.contract.ShopSelectionUiState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingGuideContentTest {
    private val selection =
        ShopSelectionUiState(
            suggestedShops = RamenShops((1..20).map { ramenShopFixture(id = "shop-$it", name = "라멘 $it") }),
            hasPersonalization = true,
            bookmarks = OnboardingBookmarks(setOf("shop-1", "shop-2", "shop-3")),
        )
    private val verticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `권한 화면은 스와이프 대상이 아니며 첫 안내에서도 권한 화면으로 돌아가지 않는다`() =
        runComposeUiTest {
            var step by mutableStateOf(OnboardingStep.PermissionGuide)
            val intents = mutableListOf<OnboardingIntent>()
            setContent {
                TestContent {
                    OnboardingContent(
                        uiState = OnboardingUiState(step = step),
                        onIntent = intents::add,
                        guideContent = { OnboardingGuideContent(uiState = selection, onIntent = {}) },
                    )
                }
            }
            onRoot().performTouchInput { swipeLeft() }
            onRoot().performTouchInput { swipeRight() }
            onNodeWithText("앱 접근 권한이 필요해요.").assertIsDisplayed()
            runOnIdle { step = OnboardingStep.Guide }
            onRoot().performTouchInput { swipeRight() }
            onNodeWithText("매장 더보기에서 알림을 설정할 수 있어요.").assertIsDisplayed()
            runOnIdle { assertTrue(intents.isEmpty()) }
        }

    @Test
    fun `안내를 옆으로 드래그하는 동안 인디케이터는 같은 위치에 표시된다`() =
        runComposeUiTest {
            setContent {
                TestContent { OnboardingGuideContent(uiState = selection, onIntent = {}) }
            }
            val indicator = onNodeWithContentDescription("사용법 안내 1 / 3")
            val message = onNodeWithText("매장 더보기에서 알림을 설정할 수 있어요.")
            val indicatorBounds = indicator.fetchSemanticsNode().boundsInRoot
            val messageLeft = message.fetchSemanticsNode().boundsInRoot.left

            onRoot().performTouchInput {
                down(center)
                moveBy(Offset(-width / 8f, 0f))
                moveBy(Offset(-width / 8f, 0f))
            }
            assertTrue(message.fetchSemanticsNode().boundsInRoot.left < messageLeft)
            assertEquals(indicatorBounds, indicator.fetchSemanticsNode().boundsInRoot)
            onRoot().performTouchInput { cancel() }

            onNodeWithText("다음").performClick()
            val nextIndicator = onNodeWithContentDescription("사용법 안내 2 / 3")
            assertEquals(indicatorBounds, nextIndicator.fetchSemanticsNode().boundsInRoot)
        }

    @Test
    fun `스와이프는 안내 안에서만 이동하고 건너뛰면 필수 매장 선택을 표시한다`() =
        runComposeUiTest {
            var state by mutableStateOf(selection.copy(bookmarks = OnboardingBookmarks(setOf("shop-1", "shop-2"))))
            val intents = mutableListOf<ShopSelectionIntent>()
            val selectionHeading = onNodeWithText("가보고 싶은 라멘집\n3곳을 저장해 주세요")
            val reviewGuide = onNodeWithText("리뷰를 남기면\n나만의 라멘 지도가 채워져요")
            setContent {
                TestContent { OnboardingGuideContent(uiState = state, onIntent = intents::add) }
            }
            onNodeWithText("다음").performClick()
            onNodeWithText("이벤트와 영업 변동 중\n받고 싶은 소식만 골라보세요.").assertIsDisplayed()
            onNodeWithText("다음").performClick()
            reviewGuide.assertIsDisplayed()
            onNodeWithContentDescription("리뷰를 남긴 라멘집이 사진으로 표시된 지도 예시 화면").assertIsDisplayed()
            onNodeWithContentDescription("사용법 안내 3 / 3").assertIsDisplayed()
            onRoot().performTouchInput { swipeLeft() }
            reviewGuide.assertIsDisplayed()
            selectionHeading.assertDoesNotExist()
            onRoot().performTouchInput { swipeRight() }
            onNodeWithText("이벤트와 영업 변동 중\n받고 싶은 소식만 골라보세요.").assertIsDisplayed()
            onRoot().performTouchInput { swipeRight() }
            onNodeWithText("매장 더보기에서 알림을 설정할 수 있어요.").assertIsDisplayed()
            val startButton = onNodeWithText("시작하기")
            onNodeWithText("건너뛰기").performClick()
            selectionHeading.assertIsDisplayed()
            startButton.assertDoesNotExist()
            onNodeWithText("2 / 3").assertDoesNotExist()
            onNodeWithContentDescription("사용법 안내 1 / 3").assertDoesNotExist()
            onRoot().performTouchInput { swipeLeft() }
            selectionHeading.assertIsDisplayed()
            onRoot().performTouchInput { swipeRight() }
            selectionHeading.assertIsDisplayed()
            reviewGuide.assertDoesNotExist()
            startButton.assertDoesNotExist()
            runOnIdle {
                assertTrue(intents.isEmpty())
                state = selection
            }
            startButton.assertIsDisplayed().assertIsEnabled().performClick()
            runOnIdle { assertEquals(listOf<ShopSelectionIntent>(ShopSelectionIntent.Complete), intents) }
        }

    @Test
    fun `매장 선택에서 이전 안내를 보고 돌아와도 검색어와 저장 상태를 유지한다`() =
        runComposeUiTest {
            var state by mutableStateOf(selection)
            val input =
                object : NavigationEventInput() {
                    fun completeBack() = dispatchOnBackCompleted()
                }
            setContent {
                TestContent(input) {
                    OnboardingGuideContent(
                        uiState = state,
                        onIntent = { if (it is ShopSelectionIntent.QueryChanged) state = state.copy(query = it.query) },
                    )
                }
            }
            onNodeWithText("건너뛰기").performClick()
            onNode(hasSetTextAction()).performTextInput("라멘")
            runOnIdle { input.completeBack() }
            onNodeWithText("리뷰를 남기면\n나만의 라멘 지도가 채워져요").assertIsDisplayed()
            onNodeWithText("다음").performClick()
            onNode(hasSetTextAction()).assertTextContains("라멘")
            onNodeWithText("시작하기").assertIsEnabled()
            runOnIdle { assertEquals(3, state.bookmarks.savedCount) }
        }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `안내 이미지와 매장 목록의 세로 스크롤은 페이지를 넘기지 않는다`() =
        runComposeUiTest {
            setContent {
                TestContent { OnboardingGuideContent(uiState = selection, onIntent = {}) }
            }
            onRoot().performTouchInput { swipeLeft() }
            onNodeWithText("이벤트와 영업 변동 중\n받고 싶은 소식만 골라보세요.").assertIsDisplayed()
            onNode(verticalScroll).performTouchInput { swipeUp() }
            val imageScroll = onNode(verticalScroll).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            runOnIdle { assertTrue(imageScroll.value() > 0f) }
            onNodeWithText("건너뛰기").assertIsDisplayed()
            onNodeWithText("다음").assertIsDisplayed().performClick()
            onNodeWithText("리뷰를 남기면\n나만의 라멘 지도가 채워져요").assertIsDisplayed()
            onNodeWithText("다음").assertIsDisplayed().performClick()
            onNode(verticalScroll).performTouchInput { swipeUp() }
            val shopScroll = onNode(verticalScroll).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            runOnIdle { assertTrue(shopScroll.value() > 0f) }
            onNodeWithText("시작하기").assertIsDisplayed()
        }

    @Test
    fun `시스템 뒤로 가기는 저장 화면에서 안내로 이동하고 완료 처리 중에는 막는다`() =
        runComposeUiTest {
            var state by mutableStateOf(selection)
            val input =
                object : NavigationEventInput() {
                    fun completeBack() = dispatchOnBackCompleted()

                    fun cancelBack() = dispatchOnBackCancelled()
                }
            setContent {
                TestContent(input) { OnboardingGuideContent(uiState = state, onIntent = {}) }
            }
            onNodeWithText("건너뛰기").performClick()
            runOnIdle { input.cancelBack() }
            onNodeWithText("시작하기").assertIsDisplayed()
            runOnIdle { input.completeBack() }
            onNodeWithText("리뷰를 남기면\n나만의 라멘 지도가 채워져요").assertIsDisplayed()
            onNodeWithText("다음").performClick()
            for (blocked in listOf(selection.copy(loadState = LoadState.loading(ShopSelectionLoadKey.Complete)), selection.copy(isCompleted = true))) {
                runOnIdle { state = blocked }
                onRoot().performTouchInput { swipeRight() }
                runOnIdle { input.completeBack() }
                onNodeWithText("가보고 싶은 라멘집\n3곳을 저장해 주세요").assertIsDisplayed()
            }
            runOnIdle { state = selection }
            runOnIdle { input.completeBack() }
            onNodeWithText("리뷰를 남기면\n나만의 라멘 지도가 채워져요").assertIsDisplayed()
        }

    @Composable
    private fun TestContent(
        input: NavigationEventInput? = null,
        content: @Composable () -> Unit,
    ) {
        val dispatcher = remember { NavigationEventDispatcher() }
        DisposableEffect(dispatcher, input) {
            if (input != null) dispatcher.addInput(input)
            onDispose { dispatcher.dispose() }
        }
        val owner =
            remember(dispatcher) {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            }
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            RamapTheme(content)
        }
    }
}
