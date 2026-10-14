package com.peto.ramap.ui.shop.menu

import androidx.lifecycle.ViewModelStore
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopMenuViewModelTest {
    @Test
    fun `조회가 끝날 때까지 로딩을 표시하고 성공하면 메뉴와 갱신일을 보관한다`() =
        menuScenario { viewModel, repository, _ ->
            val response = CompletableDeferred<RamapResult<ShopMenu>>()
            repository.handler = { response.await() }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.menu)
            val menu = ShopMenu(emptyList(), "2026-10-14T12:00:00Z")
            response.complete(RamapResult.Success(menu))
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.hasLoadFailure)
            assertEquals(menu, viewModel.uiState.value.menu)
        }

    @Test
    fun `같은 매장을 다시 선택하면 중복 요청하지 않고 다른 매장은 이전 메뉴를 비운다`() =
        menuScenario { viewModel, repository, _ ->
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("first"))
            runCurrent()
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("first"))
            runCurrent()
            assertEquals(listOf("first"), repository.calls)
            val response = CompletableDeferred<RamapResult<ShopMenu>>()
            repository.handler = { response.await() }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("next"))
            runCurrent()
            assertNull(viewModel.uiState.value.menu)
            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(listOf("first", "next"), repository.calls)
            response.complete(RamapResult.Success(ShopMenu(emptyList())))
            runCurrent()
        }

    @Test
    fun `실패 후 재시도하면 현재 매장을 다시 조회하고 오류 상태를 해제한다`() =
        menuScenario { viewModel, repository, _ ->
            repository.handler = { RamapResult.Error(RamapError.Unknown(IllegalStateException("조회 실패 픽스처"))) }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasLoadFailure)
            assertFalse(viewModel.uiState.value.isLoading)
            repository.handler = { RamapResult.Success(ShopMenu(emptyList(), "latest")) }
            viewModel.dispatch(ShopMenuIntent.OnRetry)
            runCurrent()
            assertEquals(listOf("shop", "shop"), repository.calls)
            assertFalse(viewModel.uiState.value.hasLoadFailure)
            assertEquals(
                "latest",
                viewModel.uiState.value.menu
                    ?.updatedAt,
            )
        }

    @Test
    fun `취소를 무시한 이전 매장의 성공 응답이 새 메뉴를 덮지 않는다`() =
        menuScenario { viewModel, repository, _ ->
            val response = CompletableDeferred<RamapResult<ShopMenu>>()
            repository.handler = { withContext(NonCancellable) { response.await() } }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("first"))
            runCurrent()
            repository.handler = { RamapResult.Success(ShopMenu(emptyList(), "latest")) }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("next"))
            runCurrent()
            response.complete(RamapResult.Success(ShopMenu(emptyList(), "old")))
            runCurrent()
            assertEquals("next", viewModel.uiState.value.shopId)
            assertEquals(
                "latest",
                viewModel.uiState.value.menu
                    ?.updatedAt,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `재시도로 성공한 뒤 이전 요청이 실패해도 오류로 되돌리지 않는다`() =
        menuScenario { viewModel, repository, _ ->
            val response = CompletableDeferred<RamapResult<ShopMenu>>()
            repository.handler = { withContext(NonCancellable) { response.await() } }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("shop"))
            runCurrent()
            repository.handler = { RamapResult.Success(ShopMenu(emptyList(), "latest")) }
            viewModel.dispatch(ShopMenuIntent.OnRetry)
            runCurrent()
            response.complete(RamapResult.Error(RamapError.Unknown(IllegalStateException("늦은 오류 픽스처"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasLoadFailure)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "latest",
                viewModel.uiState.value.menu
                    ?.updatedAt,
            )
        }

    @Test
    fun `소유 화면 종료 후 늦은 응답과 재시도는 메뉴를 다시 채우지 않는다`() =
        menuScenario { viewModel, repository, store ->
            val response = CompletableDeferred<RamapResult<ShopMenu>>()
            repository.handler = { withContext(NonCancellable) { response.await() } }
            viewModel.dispatch(ShopMenuIntent.OnShopSelected("shop"))
            runCurrent()
            store.clear()
            response.complete(RamapResult.Success(ShopMenu(emptyList(), "old")))
            viewModel.dispatch(ShopMenuIntent.OnRetry)
            runCurrent()
            assertNull(viewModel.uiState.value.menu)
            assertEquals(listOf("shop"), repository.calls)
        }
}

private fun menuScenario(block: suspend TestScope.(ShopMenuViewModel, FakeShopMenuRepository, ViewModelStore) -> Unit) =
    coroutinesTest {
        val repository = FakeShopMenuRepository()
        val viewModel = ShopMenuViewModel(repository)
        val store = ViewModelStore()
        store.put("menu", viewModel)
        try {
            block(viewModel, repository, store)
        } finally {
            store.clear()
        }
    }
