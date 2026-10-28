package com.peto.ramap

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
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
import com.peto.ramap.ui.login.LoginGuideController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewSearchLoginFlowTest {
    @Test
    fun `로그인 상태에서 리뷰 작성을 누르면 바로 매장 찾기로 이동한다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)

            overlay.openReviewShopSearch()

            assertEquals(ScreenRoutes.ReviewShopSearchRoutes, navigation.currentRoute)
            assertNull(guide.request.value)
        }

    @Test
    fun `로그인 성공과 세션 반영이 모두 끝나면 매장 찾기로 한 번 이동한다`() =
        runTest {
            val login = FakeLoginRepository()
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)

            overlay.openReviewShopSearch()
            val request = assertNotNull(guide.request.value)
            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)
            guide.select(request, LoginType.KAKAO)
            runCurrent()
            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)

            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            overlay.resumeReviewShopSearch()
            overlay.resumeReviewShopSearch()

            assertEquals(listOf(ScreenRoutes.FeedTabRoutes, ScreenRoutes.ReviewShopSearchRoutes), navigation.currentBackStack.toList())
        }

    @Test
    fun `세션이 먼저 바뀌어도 로그인 성공 전에 이동하지 않는다`() =
        runTest {
            val session = FakeLoginRepository()
            val pending = CompletableDeferred<RamapResult<Unit>>()
            val login =
                object : LoginRepository by session {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> = pending.await()
                }
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)
            overlay.openReviewShopSearch()
            guide.select(assertNotNull(guide.request.value), LoginType.KAKAO)
            runCurrent()

            session.updateSessionState(LoginSessionState.AUTHENTICATED)
            overlay.resumeReviewShopSearch()
            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)
            pending.complete(RamapResult.Success(Unit))
            runCurrent()

            assertEquals(ScreenRoutes.ReviewShopSearchRoutes, navigation.currentRoute)
        }

    @Test
    fun `로그인 안내를 닫거나 로그인이 실패하면 나중에 로그인해도 이동하지 않는다`() =
        runTest {
            val login = FakeLoginRepository()
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)
            overlay.openReviewShopSearch()
            guide.dismiss(assertNotNull(guide.request.value))
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            overlay.resumeReviewShopSearch()
            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)

            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            login.signInResult = RamapResult.Error(RamapError.Http(500))
            overlay.openReviewShopSearch()
            guide.select(assertNotNull(guide.request.value), LoginType.KAKAO)
            runCurrent()
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            overlay.resumeReviewShopSearch()
            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)
        }

    @Test
    fun `외부 로그인 취소 뒤 다른 로그인으로 매장 찾기가 열리지 않는다`() =
        runTest {
            val session = FakeLoginRepository()
            val pending = CompletableDeferred<RamapResult<Unit>>()
            val login =
                object : LoginRepository by session {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> = pending.await()
                }
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)
            overlay.openReviewShopSearch()
            guide.select(assertNotNull(guide.request.value), LoginType.KAKAO)
            runCurrent()

            pending.cancel()
            runCurrent()
            session.updateSessionState(LoginSessionState.AUTHENTICATED)
            overlay.resumeReviewShopSearch()

            assertEquals(ScreenRoutes.FeedTabRoutes, navigation.currentRoute)
        }

    @Test
    fun `로그인 중 피드를 떠나면 늦은 성공으로 다른 탭을 변경하지 않는다`() =
        runTest {
            val session = FakeLoginRepository()
            val pending = CompletableDeferred<RamapResult<Unit>>()
            val login =
                object : LoginRepository by session {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> = pending.await()
                }
            val navigation = navigationState()
            val guide = LoginGuideController()
            val overlay = overlayState(login, navigation, guide, backgroundScope)
            overlay.openReviewShopSearch()
            guide.select(assertNotNull(guide.request.value), LoginType.APPLE)
            runCurrent()
            navigation.selectTopLevelTab(TabStatus.MY)
            overlay.cancelPendingReviewShopSearch()
            session.updateSessionState(LoginSessionState.AUTHENTICATED)
            pending.complete(RamapResult.Success(Unit))
            runCurrent()

            assertEquals(TabStatus.MY, navigation.selectedTab)
            assertEquals(ScreenRoutes.MyTabRoutes, navigation.currentRoute)
        }

    @Test
    fun `로그아웃하면 방문 지도와 그 위의 화면을 지우고 다른 탭은 유지한다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val navigation = navigationState()
            navigation.selectTopLevelTab(TabStatus.MAP)
            navigation.showArchiveMap()
            navigation.showReviewShopSearch()
            navigation.selectTopLevelTab(TabStatus.MY)
            backgroundScope.launch { observeArchiveSession(login, navigation) }
            runCurrent()

            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()

            assertEquals(ScreenRoutes.MyTabRoutes, navigation.currentRoute)
            navigation.selectTopLevelTab(TabStatus.MAP)
            assertEquals(listOf(ScreenRoutes.MapRoutes()), navigation.currentBackStack.toList())
        }

    private fun overlayState(
        login: LoginRepository,
        navigation: NavigationState,
        guide: LoginGuideController,
        scope: CoroutineScope,
    ) = OverlayState(
        loginRepository = login,
        navigationState = navigation,
        signInUseCase = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter())),
        toastManager = ToastManager(),
        coroutineScope = scope,
        loginGuideController = guide,
    )

    private fun navigationState() =
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
}
