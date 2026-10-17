package com.peto.ramap.ui.shop.overview

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {
    @Test
    fun `로그인 상태로 상세를 열면 로그인 상태를 표시하고 한 번 조회한다`() =
        overviewScenario(initialSessionState = LoginSessionState.AUTHENTICATED) { fixture ->
            assertTrue(fixture.viewModel.uiState.value.isLoggedIn)
            assertEquals(listOf("shop"), fixture.requestedShopIds)
        }

    @Test
    fun `상세 안에서 로그인하면 대기 액션이 없어도 상세를 다시 조회한다`() =
        overviewScenario { fixture ->
            assertFalse(fixture.viewModel.uiState.value.isLoggedIn)
            fixture.fetchOverview = { RamapResult.Success(ShopOverView(ramenShopFixture(id = "shop"), 7L, null, null, null)) }
            signInFromGuide(fixture)
            assertTrue(fixture.viewModel.uiState.value.isLoggedIn)
            assertEquals(listOf("shop", "shop"), fixture.requestedShopIds)
            assertEquals(
                7L,
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount,
            )
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
        }

    @Test
    fun `로그인에 성공해도 개인화 조회가 끝난 뒤에 대기 북마크를 실행한다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(LoginType.APPLE))
            runCurrent()
            fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            assertTrue(fixture.viewModel.uiState.value.isLoggedIn)
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())

            fixture.personalization.updateBookmarkedShopIds(setOf("existing"))
            runCurrent()
            assertEquals(listOf("shop" to true), fixture.personalization.bookmarkUpdateRequests)
            assertEquals(setOf("existing", "shop"), fixture.viewModel.uiState.value.bookmarkedShopIds)
        }

    @Test
    fun `로그인 버튼을 선택하고 로그인하면 대기하던 북마크를 한 번만 실행한다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
            signInFromGuide(fixture)
            fixture.personalization.updateBookmarkedShopIds(setOf("shop", "another"))
            runCurrent()
            assertEquals(listOf("shop" to true), fixture.personalization.bookmarkUpdateRequests)
        }

    @Test
    fun `로그인 후 대기하던 매장 숨김을 실행한다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnHiddenToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            signInFromGuide(fixture)
            assertEquals(setOf("shop"), fixture.viewModel.uiState.value.hiddenShopIds)
        }

    @Test
    fun `로그인 후 대기하던 매장 알림을 실행한다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            signInFromGuide(fixture)
            assertEquals(setOf("shop"), fixture.viewModel.uiState.value.notificationShopIds)
        }

    @Test
    fun `로그인 안내를 취소하면 나중에 로그인해도 대기 액션을 실행하지 않는다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed)
            runCurrent()
            fixture.authenticate()
            runCurrent()
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
        }

    @Test
    fun `로그인 후 재개한 북마크가 저장 중이면 반대 탭을 무시하고 완료 후에는 해제한다`() = bookmarkToggleScenario(requiresLogin = true)

    @Test
    fun `로그인 상태에서 누른 북마크도 저장 중에는 반대 탭을 무시하고 완료 후에는 해제한다`() = bookmarkToggleScenario(requiresLogin = false)

    @Test
    fun `로그인 후 개인화를 기다리다 상세를 닫으면 대기 액션을 실행하지 않는다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(LoginType.APPLE))
            fixture.login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            fixture.store.clear()
            runCurrent()
            fixture.authenticate()
            runCurrent()
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
        }

    @Test
    fun `저장소를 비우면 예약된 로그인 알림과 이후 액션을 처리하지 않는다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(LoginType.APPLE))
            runCurrent()
            fixture.authenticate()
            fixture.store.clear()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "other")))
            runCurrent()
            assertFalse(fixture.viewModel.viewModelScope.isActive)
            assertEquals(listOf("shop"), fixture.requestedShopIds)
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
        }

    @Test
    fun `저장소를 비우면 이미 재개된 로그인 대기 요청도 취소한다`() =
        overviewScenario { fixture ->
            val response = CompletableDeferred<Unit>()
            var completed = false
            var finished = false
            fixture.awaitPersonalizationResponse = {
                try {
                    response.await()
                    completed = true
                } finally {
                    finished = true
                }
            }
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            signInFromGuide(fixture)
            assertEquals(setOf("existing", "shop"), fixture.viewModel.uiState.value.bookmarkedShopIds)
            fixture.store.clear()
            response.complete(Unit)
            runCurrent()
            assertTrue(finished)
            assertFalse(completed)
            assertFalse(fixture.viewModel.viewModelScope.isActive)
        }

    @Test
    fun `같은 매장 재조회 후 취소를 무시한 이전 성공은 최신 상태를 덮지 않는다`() = lateOverviewScenario(RamapResult.Success(ShopOverView(ramenShopFixture(id = "shop"), 99L, null, null, null)))

    @Test
    fun `같은 매장 재조회 후 취소를 무시한 이전 실패는 오류로 되돌리지 않는다`() = lateOverviewScenario(RamapResult.Error(RamapError.Http(500)))

    @Test
    fun `상세 안에서 로그인한 뒤 늦게 도착한 비로그인 조회 성공을 무시한다`() =
        lateOverviewScenario(
            oldResult = RamapResult.Success(ShopOverView(ramenShopFixture(id = "shop"), 99L, null, null, null)),
            loginBeforeResponse = true,
        )

    @Test
    fun `상세 안에서 로그인한 뒤 늦게 도착한 비로그인 조회 실패를 무시한다`() =
        lateOverviewScenario(
            oldResult = RamapResult.Error(RamapError.Http(500)),
            loginBeforeResponse = true,
        )

    private fun bookmarkToggleScenario(requiresLogin: Boolean) =
        overviewScenario { fixture ->
            val response = CompletableDeferred<Unit>()
            fixture.awaitPersonalizationResponse = { response.await() }
            if (!requiresLogin) {
                fixture.authenticate()
                runCurrent()
            }
            val shop = ramenShopFixture(id = "shop")
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
            runCurrent()
            if (requiresLogin) signInFromGuide(fixture)
            assertEquals(setOf("existing", "shop"), fixture.viewModel.uiState.value.bookmarkedShopIds)

            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
            runCurrent()
            assertEquals(listOf("shop" to true), fixture.personalization.bookmarkUpdateRequests)
            assertTrue("shop" in fixture.viewModel.uiState.value.bookmarkedShopIds)

            response.complete(Unit)
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
            runCurrent()
            assertEquals(listOf("shop" to true, "shop" to false), fixture.personalization.bookmarkUpdateRequests)
            assertFalse("shop" in fixture.viewModel.uiState.value.bookmarkedShopIds)
        }

    @Test
    fun `상세 저장소를 비우면 진행 중인 북마크 알림 숨김 요청을 모두 취소한다`() =
        overviewScenario { fixture ->
            fixture.authenticate()
            runCurrent()
            val response = CompletableDeferred<Unit>()
            var started = 0
            var finished = 0
            var completed = 0
            fixture.awaitPersonalizationResponse = {
                started++
                try {
                    response.await()
                    completed++
                } finally {
                    finished++
                }
            }
            val shop = ramenShopFixture(id = "shop")
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(shop))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnHiddenToggled(shop))
            runCurrent()
            assertEquals(3, started)
            assertTrue("shop" in fixture.viewModel.uiState.value.hiddenShopIds)

            fixture.store.clear()
            runCurrent()
            assertEquals(3, finished)
            response.complete(Unit)
            runCurrent()
            assertEquals(0, completed)

            assertFalse(fixture.viewModel.viewModelScope.isActive)
        }

    private fun lateOverviewScenario(
        oldResult: RamapResult<ShopOverView>,
        loginBeforeResponse: Boolean = false,
    ) = overviewScenario { fixture ->
        val oldResponse = CompletableDeferred<RamapResult<ShopOverView>>()
        var oldReturned = false
        fixture.fetchOverview = {
            withContext(NonCancellable) { oldResponse.await() }.also { oldReturned = true }
        }
        try {
            fixture.viewModel.dispatch(ShopOverviewIntent.OnOverviewRetry)
            runCurrent()
            fixture.fetchOverview = { RamapResult.Success(ShopOverView(ramenShopFixture(id = "shop"), 7L, null, null, null)) }
            if (loginBeforeResponse) signInFromGuide(fixture) else fixture.viewModel.dispatch(ShopOverviewIntent.OnOverviewRetry)
            runCurrent()
            assertEquals(listOf("shop", "shop", "shop"), fixture.requestedShopIds)
            assertEquals(
                7L,
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount,
            )
            oldResponse.complete(oldResult)
            runCurrent()
            assertTrue(oldReturned, "취소된 요청도 실제로 늦게 반환했다")
            assertEquals(
                7L,
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount,
            )
            assertFalse(fixture.viewModel.uiState.value.hasLoadFailure)
            assertFalse(fixture.viewModel.uiState.value.isLoading)
        } finally {
            oldResponse.complete(oldResult)
        }
    }

    private fun TestScope.signInFromGuide(fixture: OverviewViewModelFixture) {
        fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(LoginType.APPLE))
        runCurrent()
        assertEquals(1, fixture.login.signInWithAppleCallCount)
        fixture.authenticate()
        runCurrent()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun overviewScenario(
    initialSessionState: LoginSessionState = LoginSessionState.NOT_AUTHENTICATED,
    block: suspend TestScope.(OverviewViewModelFixture) -> Unit,
) = coroutinesTest {
    val fixture = OverviewViewModelFixture(initialSessionState)
    try {
        fixture.viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected("shop"))
        runCurrent()
        block(fixture)
    } finally {
        fixture.store.clear()
    }
}
