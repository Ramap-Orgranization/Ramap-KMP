package com.peto.ramap.ui.shop

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewBookmarkCountRegressionTest {
    @Test
    fun `로그인 후 저장보다 먼저 조회한 응답이 늦게 도착해도 상세 저장 수를 유지한다`() = delayedLoginOverviewScenario(RamapResult.Success(22L))

    @Test
    fun `로그인 재조회 응답에 이미 저장이 반영돼 있어도 상세 저장 수를 중복 증가시키지 않는다`() = delayedLoginOverviewScenario(RamapResult.Success(23L))

    @Test
    fun `로그인 재조회가 실패해도 재개한 저장 수와 기존 상세를 유지한다`() = delayedLoginOverviewScenario(RamapResult.Error(RamapError.Http(500)))

    @Test
    fun `로그인 상세 재조회가 먼저 끝나도 이후 재개한 저장 수를 반영한다`() =
        scenario { fixture ->
            requestBookmarkAndLogin(fixture)
            assertEquals(listOf("shop", "shop"), fixture.requestedShopIds)
            assertEquals(
                22L,
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount,
            )

            fixture.personalization.updateBookmarkedShopIds(emptySet())
            runCurrent()

            assertEquals(listOf("shop" to true), fixture.personalization.bookmarkUpdateRequests)
            assertCount(fixture, 23L)
        }

    @Test
    fun `로그인 후 이미 저장한 매장임을 알게 되면 저장 수를 증가시키지 않는다`() =
        scenario { fixture ->
            requestBookmarkAndLogin(fixture)
            fixture.personalization.updateBookmarkedShopIds(setOf("shop"))
            runCurrent()

            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
            assertCount(fixture, 22L)
        }

    @Test
    fun `로그인 안내를 취소한 뒤 로그인하면 상세 재조회만 하고 저장 수는 유지한다`() =
        scenario { fixture ->
            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(fixture.shop))
            fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed)
            runCurrent()
            fixture.authenticate()
            fixture.personalization.updateBookmarkedShopIds(emptySet())
            runCurrent()

            assertEquals(listOf("shop", "shop"), fixture.requestedShopIds)
            assertTrue(fixture.personalization.bookmarkUpdateRequests.isEmpty())
            assertCount(fixture, 22L)
        }

    private fun delayedLoginOverviewScenario(response: RamapResult<Long>) =
        scenario { fixture ->
            val overviewResponse = CompletableDeferred<RamapResult<ShopOverView>>()
            fixture.fetchOverview = { overviewResponse.await() }
            requestBookmarkAndLogin(fixture)
            assertEquals(listOf("shop", "shop"), fixture.requestedShopIds)

            fixture.personalization.updateBookmarkedShopIds(emptySet())
            runCurrent()
            assertEquals(listOf("shop" to true), fixture.personalization.bookmarkUpdateRequests)
            assertEquals(
                23L,
                fixture.viewModel.uiState.value.shopOverview
                    ?.likeCount,
            )

            overviewResponse.complete(
                when (response) {
                    is RamapResult.Success -> RamapResult.Success(fixture.initialOverview.copy(likeCount = response.data))
                    is RamapResult.Error -> response
                },
            )
            runCurrent()
            assertCount(fixture, 23L)

            fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(fixture.shop))
            runCurrent()
            assertEquals(listOf("shop" to true, "shop" to false), fixture.personalization.bookmarkUpdateRequests)
            assertCount(fixture, 22L)
            assertEquals(listOf("shop", "shop"), fixture.requestedShopIds)
        }

    private fun TestScope.requestBookmarkAndLogin(fixture: OverviewBookmarkCountFixture) {
        fixture.viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(fixture.shop))
        fixture.viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(LoginType.APPLE))
        runCurrent()
        fixture.authenticate()
        runCurrent()
    }

    private fun assertCount(
        fixture: OverviewBookmarkCountFixture,
        expected: Long,
    ) {
        assertEquals(
            expected,
            fixture.viewModel.uiState.value.shopOverview
                ?.likeCount,
            "열린 상세의 저장 수",
        )
        assertEquals(expected, assertIs<ShopOverviewCacheLookup.Hit>(fixture.query.findCached("shop")).overview.likeCount, "상세 캐시의 저장 수")
    }

    private fun scenario(block: suspend TestScope.(OverviewBookmarkCountFixture) -> Unit) =
        coroutinesTest {
            val fixture = OverviewBookmarkCountFixture()
            try {
                fixture.viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected("shop"))
                runCurrent()
                block(fixture)
            } finally {
                fixture.close()
            }
        }
}
