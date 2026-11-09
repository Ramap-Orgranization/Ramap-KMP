package com.peto.ramap.ui.shop.overview

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_notification_description
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {
    @Test
    fun `비회원 저장은 별표만 바꾸고 매장 알림은 로그인 안내를 보낸다`() =
        overviewScenario { fixture ->
            val shop = ramenShopFixture(id = "shop")
            val initialCount =
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount
            fixture.viewModel.sideEffect.test {
                assertEquals(ShopOverviewSideEffect.ShopViewed(shop), awaitItem())

                fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
                runCurrent()
                expectNoEvents()
                assertEquals(listOf(shop.id to true), fixture.personalization.bookmarkUpdateRequests)
                assertTrue(shop.id in fixture.viewModel.uiState.value.bookmarkedShopIds)
                assertEquals(
                    initialCount,
                    fixture.viewModel.uiState.value.shopOverview
                        ?.likeCount,
                )

                fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(shop))
                runCurrent()
                assertEquals(
                    ShopOverviewSideEffect.Interaction(ShopInteractionEffect.ShowLoginGuide(Res.string.login_required_notification_description)),
                    awaitItem(),
                )
                assertTrue(
                    fixture.viewModel.uiState.value.notificationShopIds
                        .isEmpty(),
                )
            }
        }

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
    fun `로그인 후 대기하던 매장 알림 설정을 열고 저장 전에는 구독을 바꾸지 않는다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            signInFromGuide(fixture)
            assertEquals("shop", fixture.viewModel.uiState.value.notificationSettingsShopId)
            assertTrue(
                fixture.viewModel.uiState.value.notificationShopIds
                    .isEmpty(),
            )
            assertTrue(fixture.notificationSaveRequests.isEmpty())
        }

    @Test
    fun `로그인 안내를 취소하면 나중에 로그인해도 대기 액션을 실행하지 않는다`() =
        overviewScenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(ramenShopFixture(id = "shop")))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed)
            runCurrent()
            fixture.authenticate()
            runCurrent()
            assertEquals(null, fixture.viewModel.uiState.value.notificationSettingsShopId)
        }

    @Test
    fun `로그인 상태에서 누른 북마크도 저장 중에는 반대 탭을 무시하고 완료 후에는 해제한다`() = bookmarkToggleScenario(requiresLogin = false)

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
    fun `상세 저장소를 비우면 진행 중인 북마크와 알림 요청을 모두 취소한다`() =
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
            fixture.awaitNotificationResponse = fixture.awaitPersonalizationResponse
            val shop = ramenShopFixture(id = "shop")
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(shop))
            runCurrent()
            fixture.viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(shop))
            runCurrent()
            assertEquals(2, started)

            fixture.store.clear()
            runCurrent()
            assertEquals(2, finished)
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
