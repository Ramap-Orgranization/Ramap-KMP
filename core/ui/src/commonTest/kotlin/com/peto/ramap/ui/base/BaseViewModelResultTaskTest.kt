package com.peto.ramap.ui.base

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelResultTaskTest {
    @Test
    fun `성공 결과는 성공 콜백에 전달하고 로딩을 해제한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val result = CompletableDeferred<RamapResult<String>>()

            viewModel.startResult(TestTaskKey.First, result)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(TestLoadKey.Request),
            )

            result.complete(RamapResult.Success("success"))
            runCurrent()

            assertEquals(listOf("success"), viewModel.uiState.value.results)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `오류 결과는 공통 처리 후 오류 콜백에 한 번 전달한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val error = RamapError.Unknown(IllegalStateException("failure"))
            val result = CompletableDeferred<RamapResult<String>>()

            viewModel.startResult(TestTaskKey.First, result)
            runCurrent()
            result.complete(RamapResult.Error(error))
            runCurrent()

            assertEquals(error, viewModel.handledResultErrors.single())
            assertEquals(error, viewModel.callbackResultErrors.single())
            assertEquals(listOf("common", "callback"), viewModel.resultErrorCalls)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `중복 무시 정책은 새 요청과 로딩 증가를 모두 위임한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val first = CompletableDeferred<RamapResult<String>>()

            val firstJob =
                viewModel.startResult(
                    TestTaskKey.First,
                    first,
                    TaskPolicy.IgnoreNew,
                )
            val ignoredJob =
                viewModel.startResult(
                    TestTaskKey.First,
                    CompletableDeferred(),
                    TaskPolicy.IgnoreNew,
                )
            runCurrent()

            assertTrue(firstJob != null)
            assertNull(ignoredJob)
            assertEquals(1, viewModel.startedCount)

            first.complete(RamapResult.Success("first"))
            runCurrent()
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    private fun viewModelTest(testBody: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                testBody()
            } finally {
                Dispatchers.resetMain()
            }
        }
}
