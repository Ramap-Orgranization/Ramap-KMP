package com.peto.ramap.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.ui.loading.LoadKey
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.retry.NetworkRetryGenerator
import com.peto.ramap.ui.task.TaskEntry
import com.peto.ramap.ui.task.TaskKey
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : State, I : Intent, SE : SideEffect>(
    initialState: S,
) : ViewModel() {
    protected open val logger: Logger =
        Logger.withTag(this::class.simpleName ?: "BaseViewModel")

    // State
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()
    protected val currentState: S get() = _uiState.value

    // SideEffect
    private val sideEffectChannel = Channel<SE>(Channel.BUFFERED)
    val sideEffect: Flow<SE> = sideEffectChannel.receiveAsFlow()

    // Intent
    private val intentChannel = Channel<I>(Channel.BUFFERED)

    private val tasks = mutableMapOf<TaskKey, TaskEntry<S>>()

    /** 취소된 이전 작업과 같은 키로 다시 선택된 작업도 구분하는 단조 증가 generation. */
    private var nextTaskGeneration = 0L

    init {
        viewModelScope.launch {
            for (intent in intentChannel) {
                try {
                    handleIntent(intent)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    handleError(throwable)
                }
            }
        }
    }

    override fun onCleared() {
        NetworkRetryGenerator.remove(this)
        super.onCleared()
    }

    /**
     * Intent를 처리하는 메서드
     * */
    protected abstract suspend fun handleIntent(intent: I)

    /**
     * UI에서 Intent를 발생시키는 메서드
     * */
    fun dispatch(intent: I) {
        val result = intentChannel.trySend(intent)
        if (result.isFailure) {
            logger.w { "이벤트 유실: $intent, 원인 = ${result.exceptionOrNull()}" }
        }
    }

    /**
     * State를 변경하는 메서드
     * */
    protected fun reduce(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /**
     * [taskKey]로 식별되는 coroutine 작업을 ViewModel 수명주기에서 실행한다.
     *
     * [loadKey]가 있으면 작업 시작 시 해당 키의 카운트를 증가시키고
     * 성공·오류·취소와 관계없이 현재 generation의 작업이 끝날 때 정확히 한 번 감소시킨다.
     * 이 경우 상태 [S]는 [LoadableState]를 구현해야 한다.
     *
     * 같은 [taskKey]의 실행 중 작업이 있으면 [policy]에 따라 기존 작업을 동기적으로 상태 정리한 뒤
     * 교체하거나 새 요청을 무시한다.
     *
     * [CancellationException]은 다시 전파하며 그 외 예외는 [handleError]로 전달한다.
     *
     * @param taskKey ViewModel 안에서 작업을 식별하는 문자열. 같은 문자열에만 중복 정책을 적용한다.
     * @param loadKey 활성 개수를 추적할 로딩 키. `null`이면 로딩 카운트를 변경하지 않는다.
     * @param policy 동일 작업 키가 실행 중일 때 적용할 정책
     * @param onStart 작업 등록과 로딩 증가 시 함께 적용할 상태 reducer
     * @param onFinish 현재 작업의 로딩 감소 시 정확히 한 번 적용할 상태 reducer
     * @param block 실행할 비동기 작업
     * @return 시작한 [Job], 또는 [TaskPolicy.IgnoreNew]로 요청을 무시한 경우 `null`
     */
    protected fun launchTask(
        taskKey: String,
        loadKey: LoadKey? = null,
        policy: TaskPolicy = TaskPolicy.CancelPrevious,
        onStart: S.() -> S = { this },
        onFinish: S.() -> S = { this },
        block: suspend () -> Unit,
    ): Job? =
        launchTaskInternal(
            taskKey = TaskKey(taskKey),
            loadKey = loadKey,
            policy = policy,
            onStart = onStart,
            onFinish = onFinish,
            block = block,
        )

    private fun launchTaskInternal(
        taskKey: TaskKey,
        loadKey: LoadKey?,
        policy: TaskPolicy,
        onStart: S.() -> S,
        onFinish: S.() -> S,
        block: suspend () -> Unit,
    ): Job? {
        val previousTask = tasks[taskKey]
        if (previousTask != null) {
            if (policy == TaskPolicy.IgnoreNew) return null
            finishTask(taskKey, previousTask, shouldCancel = true)
        }

        val generation = ++nextTaskGeneration
        updateTaskState(loadKey, isStarting = true, reducer = onStart)
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    block()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    handleError(throwable)
                } finally {
                    completeTask(taskKey, generation)
                }
            }
        tasks[taskKey] =
            TaskEntry(
                generation = generation,
                job = job,
                loadKey = loadKey,
                onFinish = onFinish,
            )
        job.start()
        return job
    }

    /**
     * 단일 [RamapResult] 요청을 작업 정책과 로딩 상태 관리 안에서 실행한다.
     *
     * 실행·취소·중복·로딩 정리는 [launchTask]에 위임한다. [RamapResult.Error]이면 공통
     * [handleError]를 한 번 호출한 뒤 [onError]를 호출하며, 요청이나 콜백에서 발생한 예외는
     * [launchTask]의 예외 처리 계약을 그대로 따른다.
     *
     * @param taskKey ViewModel 안에서 요청 작업을 식별하는 문자열
     * @param loadKey 활성 개수를 추적할 로딩 키. `null`이면 로딩 카운트를 변경하지 않는다.
     * @param policy 동일 작업 키가 실행 중일 때 적용할 정책
     * @param onStart 작업 등록과 로딩 증가 시 함께 적용할 상태 reducer
     * @param onFinish 현재 작업의 로딩 감소 시 정확히 한 번 적용할 상태 reducer
     * @param request 한 번 실행해 [RamapResult]를 반환할 요청
     * @param onSuccess 성공 데이터를 처리할 콜백
     * @param onError 공통 오류 처리 후 도메인 오류를 처리할 콜백
     * @param retryOnNetworkError 네트워크 오류를 연결 복구 후 다시 실행할지 여부
     * @return 시작한 [Job], 또는 [TaskPolicy.IgnoreNew]로 요청을 무시한 경우 `null`
     */
    protected fun <T> launchResultTask(
        taskKey: String,
        loadKey: LoadKey? = null,
        policy: TaskPolicy = TaskPolicy.CancelPrevious,
        onStart: S.() -> S = { this },
        onFinish: S.() -> S = { this },
        retryOnNetworkError: Boolean = false,
        request: suspend () -> RamapResult<T>,
        onSuccess: suspend (T) -> Unit = {},
        onError: suspend (RamapError) -> Unit = {},
    ): Job? {
        NetworkRetryGenerator.remove(this, taskKey)
        return launchTask(
            taskKey = taskKey,
            loadKey = loadKey,
            policy = policy,
            onStart = onStart,
            onFinish = onFinish,
        ) {
            when (val result = request()) {
                is RamapResult.Success -> {
                    NetworkRetryGenerator.remove(this@BaseViewModel, taskKey)
                    onSuccess(result.data)
                }
                is RamapResult.Error -> {
                    handleError(result.error)
                    if (retryOnNetworkError && result.error is RamapError.Network) {
                        NetworkRetryGenerator.enqueue(this@BaseViewModel, taskKey) {
                            launchResultTask(
                                taskKey = taskKey,
                                loadKey = loadKey,
                                policy = policy,
                                onStart = onStart,
                                onFinish = onFinish,
                                retryOnNetworkError = retryOnNetworkError,
                                request = request,
                                onSuccess = onSuccess,
                                onError = onError,
                            )
                        }
                    }
                    onError(result.error)
                }
            }
        }
    }

    /**
     * [taskKey]에 등록된 작업을 취소하고 종료 상태를 동기적으로 정리한다.
     *
     * 작업이 있으면 레지스트리에서 먼저 제거한 뒤 로딩 카운트와 `onFinish`를 한 번 정리하므로,
     * 취소된 coroutine의 늦은 `finally`는 상태를 다시 변경하지 않는다. 등록된 작업이 없으면 아무 일도 하지 않는다.
     *
     * @param taskKey 취소할 ViewModel 로컬 작업의 식별 문자열
     */
    protected fun cancelTask(taskKey: String) {
        val registryKey = TaskKey(taskKey)
        NetworkRetryGenerator.remove(this, taskKey)
        val task = tasks[registryKey] ?: return
        finishTask(registryKey, task, shouldCancel = true)
    }

    /**
     * SideEffect를 발생시키는 메서드
     * */
    protected suspend fun postSideEffect(effect: SE) {
        sideEffectChannel.send(effect)
    }

    protected fun trySideEffect(effect: SE) {
        sideEffectChannel.trySend(effect)
    }

    protected suspend fun <T> handleResult(
        result: RamapResult<T>,
        onSuccess: suspend (T) -> Unit = {},
        onError: suspend (RamapError) -> Unit = {},
    ) {
        when (result) {
            is RamapResult.Success -> onSuccess(result.data)
            is RamapResult.Error -> {
                handleError(result.error)
                onError(result.error)
            }
        }
    }

    protected open fun handleError(error: RamapError) {
        logger.e(error.cause) { "요청 처리 실패: $error" }
    }

    protected open fun handleError(throwable: Throwable) {
        logger.e(throwable) { "처리되지 않은 오류" }
    }

    private fun completeTask(
        taskKey: TaskKey,
        generation: Long,
    ) {
        // 교체된 이전 작업의 늦은 finally가 새 작업의 로딩을 종료하지 못하게 한다.
        val task = tasks[taskKey] ?: return
        if (task.generation != generation) return
        finishTask(taskKey, task, shouldCancel = false)
    }

    private fun finishTask(
        taskKey: TaskKey,
        task: TaskEntry<S>,
        shouldCancel: Boolean,
    ) {
        // 레지스트리를 먼저 제거해 취소 과정에서 실행되는 finally와 종료 정리가 중복되지 않게 한다.
        tasks.remove(taskKey)
        if (shouldCancel) task.job.cancel()
        updateTaskState(task.loadKey, isStarting = false, reducer = task.onFinish)
    }

    private fun updateTaskState(
        loadKey: LoadKey?,
        isStarting: Boolean,
        reducer: S.() -> S,
    ) {
        // reducer와 카운트 변경을 하나의 StateFlow 갱신으로 노출한다.
        _uiState.update { state ->
            val reducedState = state.reducer()
            if (loadKey == null) return@update reducedState

            val loadableState = reducedState.asLoadableState()
            val nextLoadingState =
                if (isStarting) {
                    loadableState.loadState + loadKey
                } else {
                    loadableState.loadState - loadKey
                }
            loadableState.withLoadingState(nextLoadingState)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun S.asLoadableState(): LoadableState<S> =
        this as? LoadableState<S>
            ?: error("${this::class.simpleName} must implement LoadableState to launch a loading task")
}
