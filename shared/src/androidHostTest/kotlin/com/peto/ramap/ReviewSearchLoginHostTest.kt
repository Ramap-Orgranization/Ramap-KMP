package com.peto.ramap

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginGuideController
import org.jetbrains.compose.resources.stringResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.kakao_login_button_title
import ramap.shared.generated.resources.login_required_description
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ReviewSearchLoginHostTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `피드에서 로그인 안내를 완료하면 매장 찾기 화면으로 이어진다`() =
        runComposeUiTest {
            val session = FakeLoginRepository()
            val login =
                object : LoginRepository by session {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> {
                        session.updateSessionState(LoginSessionState.AUTHENTICATED)
                        return RamapResult.Success(Unit)
                    }
                }
            val navigation =
                NavigationState(
                    selectedTabState = mutableStateOf(TabStatus.FEED),
                    backStacks =
                        mapOf(
                            TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.MapRoutes()),
                            TabStatus.RANKING to NavBackStack<NavKey>(ScreenRoutes.RankingTabRoutes),
                            TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes()),
                            TabStatus.FEED to NavBackStack<NavKey>(ScreenRoutes.FeedTabRoutes),
                            TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                        ),
                )
            val guide = LoginGuideController()
            val toastManager = ToastManager()
            val signIn = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter()))
            lateinit var overlay: OverlayState
            var loginButton = ""
            var loginDescription = ""
            setContent {
                overlay =
                    rememberAppOverlayState(
                        navigationState = navigation,
                        toastManager = toastManager,
                        loginRepository = login,
                        signInUseCase = signIn,
                        loginGuideController = guide,
                    )
                loginButton = stringResource(Res.string.kakao_login_button_title)
                loginDescription = stringResource(Res.string.login_required_description)
                RamapTheme { Box(Modifier.fillMaxSize()) { LoginGuideHost(guide) } }
            }

            runOnIdle { overlay.openReviewShopSearch() }
            onNodeWithText(loginDescription).assertIsDisplayed()
            runOnIdle { assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute) }
            onNodeWithText(loginButton).performClick()
            runOnIdle { assertEquals(ScreenRoutes.ReviewShopSearchRoutes, navigation.currentRoute) }
        }
}
