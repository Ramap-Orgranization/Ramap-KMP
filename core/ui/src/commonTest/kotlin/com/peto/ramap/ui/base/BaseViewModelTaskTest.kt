package com.peto.ramap.ui.base

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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTaskTest {
    @Test
    fun `같은 작업은 이전 실행을 취소하고 최신 결과만 반영한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val first = CompletableDeferred<String>()
            val latest = CompletableDeferred<String>()

            viewModel.start(TestTaskKey.First, first)
            runCurrent()
            viewModel.start(TestTaskKey.First, latest)
            runCurrent()
            latest.complete("latest")
            runCurrent()

            assertEquals(listOf("latest"), viewModel.uiState.value.results)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `중복 무시 작업은 새 실행을 시작하지 않는다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val first = CompletableDeferred<String>()

            viewModel.start(TestTaskKey.First, first, TaskPolicy.IgnoreNew)
            viewModel.start(TestTaskKey.First, CompletableDeferred(), TaskPolicy.IgnoreNew)
            runCurrent()

            assertEquals(1, viewModel.startedCount)
            first.complete("first")
            runCurrent()
            assertEquals(listOf("first"), viewModel.uiState.value.results)
        }

    @Test
    fun `서로 다른 작업이 같은 로딩 키를 공유하면 활성 개수를 보존한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val first = CompletableDeferred<String>()
            val second = CompletableDeferred<String>()

            viewModel.start(TestTaskKey.First, first)
            viewModel.start(TestTaskKey.Second, second)
            runCurrent()

            assertEquals(
                2,
                viewModel.uiState.value.loadState
                    .activeCount(TestLoadKey.Request),
            )
            first.complete("first")
            runCurrent()
            assertEquals(
                1,
                viewModel.uiState.value.loadState
                    .activeCount(TestLoadKey.Request),
            )
            second.complete("second")
            runCurrent()
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `취소된 이전 작업의 늦은 종료는 새 작업 로딩을 해제하지 않는다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val first = CompletableDeferred<String>()
            val latest = CompletableDeferred<String>()

            viewModel.start(TestTaskKey.First, first, ignoreCancellation = true)
            runCurrent()
            viewModel.start(TestTaskKey.First, latest)
            runCurrent()
            first.complete("old")
            runCurrent()

            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(TestLoadKey.Request),
            )

            latest.complete("latest")
            runCurrent()
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `명시적 취소와 예외는 로딩을 정확히 한 번 해제한다`() =
        viewModelTest {
            val viewModel = TestViewModel()
            val pending = CompletableDeferred<String>()

            viewModel.start(TestTaskKey.First, pending)
            runCurrent()
            viewModel.cancel(TestTaskKey.First)
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)

            viewModel.startFailure(TestTaskKey.Second)
            runCurrent()
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
            assertEquals(1, viewModel.errorCount)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
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
