package com.peto.ramap

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.review.write.ReviewWriteViewModel
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import kotlinx.coroutines.CompletableDeferred
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ReviewWriteOverlayLifecycleTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `저장 중 닫은 작성 화면의 작업과 완료 이벤트가 다음 편집기에 남지 않는다`() =
        runComposeUiTest {
            val shop = ramenShopFixture(id = "shop")
            val originalBody = "원래 리뷰 내용입니다"
            val started = CompletableDeferred<Unit>()
            val completion = CompletableDeferred<Unit>()
            var wasCancelled = false
            val reviews =
                object : ReviewRepository by FakeShopReviewRepository() {
                    override suspend fun fetchEditableReview(reviewId: String) = RamapResult.Success(EditableReview(reviewId, shop.id, originalBody, emptyList(), emptyList(), false))

                    override suspend fun updateReview(
                        reviewId: String,
                        body: String,
                        retainedImagePaths: List<String>,
                        newImages: List<ReviewImage>,
                        isPublic: Boolean,
                    ): RamapResult<Unit> {
                        started.complete(Unit)
                        try {
                            completion.await()
                            return RamapResult.Success(Unit)
                        } finally {
                            wasCancelled = true
                        }
                    }
                }
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val profile = FakeProfileRepository()
            val shops = FakeRamenShopRepository(shopOverview = ShopOverView(shop, 0, null, null, null))
            val toast = ToastManager()
            val created = mutableListOf<ReviewWriteViewModel>()
            val dependencies =
                koinApplication {
                    modules(
                        module {
                            single { toast }
                            factory { ReviewWriteViewModel(reviews, profile, shops).also(created::add) }
                        },
                    )
                }
            val navigation = NavigationState(mutableStateOf(TabStatus.MAP), mapOf(TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.MapRoutes())))
            val dispatcher = NavigationEventDispatcher()
            val owner =
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            val signIn = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter()))
            val guide = LoginGuideController()
            lateinit var overlay: OverlayState
            setContent {
                overlay = rememberAppOverlayState(navigation, toast, login, signIn, guide)
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    KoinIsolatedContext(dependencies) {
                        RamapTheme { OverlayHost(overlay, navigation, {}) }
                    }
                }
            }
            try {
                runOnIdle { overlay.overlayReviewWriteArgs = shop.id to "review" }
                waitForIdle()
                runOnIdle {
                    assertEquals(1, created.size, "작성 화면마다 ViewModel 하나를 만들어야 합니다")
                    val firstState = created.first().uiState.value
                    assertTrue(firstState.reviewLoaded, "첫 작성 화면 상태: $firstState")
                }
                runOnIdle { created.first().dispatch(ReviewWriteIntent.ChangeBody("저장 도중 닫을 리뷰입니다")) }
                waitUntil {
                    created
                        .first()
                        .uiState.value.body == "저장 도중 닫을 리뷰입니다"
                }
                runOnIdle { created.first().dispatch(ReviewWriteIntent.Submit) }
                waitUntil { started.isCompleted }
                runOnIdle { overlay.overlayReviewWriteArgs = null }
                waitForIdle()
                waitUntil { wasCancelled }
                completion.complete(Unit)
                runOnIdle { overlay.overlayReviewWriteArgs = shop.id to "review" }
                waitForIdle()
                waitUntil {
                    created.size == 2 &&
                        created
                            .last()
                            .uiState.value.reviewLoaded
                }
                onNodeWithText(originalBody).assertIsDisplayed()
                runOnIdle {
                    assertNotSame(created.first(), created.last())
                    assertEquals(shop.id to "review", overlay.overlayReviewWriteArgs)
                    assertTrue(wasCancelled)
                }
            } finally {
                runOnIdle { overlay.overlayReviewWriteArgs = null }
                waitForIdle()
                dependencies.close()
                dispatcher.dispose()
            }
        }
}
