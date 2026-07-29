package com.peto.ramap.ui.base

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class TestViewModel : BaseViewModel<TestState, TestIntent, TestSideEffect>(TestState()) {
    var startedCount = 0
    var errorCount = 0
    val handledResultErrors = mutableListOf<RamapError>()
    val callbackResultErrors = mutableListOf<RamapError>()
    val resultErrorCalls = mutableListOf<String>()

    override suspend fun handleIntent(intent: TestIntent) = Unit

    fun start(
        key: TestTaskKey,
        result: CompletableDeferred<String>,
        policy: TaskPolicy = TaskPolicy.CancelPrevious,
        ignoreCancellation: Boolean = false,
    ) {
        launchTask(
            taskKey = key.name,
            loadKey = TestLoadKey.Request,
            policy = policy,
        ) {
            startedCount += 1
            val value =
                if (ignoreCancellation) {
                    withContext(NonCancellable) { result.await() }
                } else {
                    result.await()
                }
            reduce { copy(results = results + value) }
        }
    }

    fun startFailure(key: TestTaskKey) {
        launchTask(
            taskKey = key.name,
            loadKey = TestLoadKey.Request,
        ) {
            error("failure")
        }
    }

    fun cancel(key: TestTaskKey) {
        cancelTask(key.name)
    }

    fun startResult(
        key: TestTaskKey,
        result: CompletableDeferred<RamapResult<String>>,
        policy: TaskPolicy = TaskPolicy.CancelPrevious,
    ): Job? =
        launchResultTask(
            taskKey = key.name,
            loadKey = TestLoadKey.Request,
            policy = policy,
            request = {
                startedCount += 1
                result.await()
            },
            onSuccess = { value -> reduce { copy(results = results + value) } },
            onError = { error ->
                resultErrorCalls += "callback"
                callbackResultErrors += error
            },
        )

    override fun handleError(error: RamapError) {
        resultErrorCalls += "common"
        handledResultErrors += error
    }

    override fun handleError(throwable: Throwable) {
        errorCount += 1
    }
}
