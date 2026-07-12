package com.peto.ramap.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import kotlinx.coroutines.CancellationException
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
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = mutableUiState.asStateFlow()
    protected val currentState: S get() = mutableUiState.value

    // SideEffect
    private val sideEffectChannel = Channel<SE>(Channel.BUFFERED)
    val sideEffect: Flow<SE> = sideEffectChannel.receiveAsFlow()

    // Intent
    private val intentChannel = Channel<I>(Channel.BUFFERED)

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
        mutableUiState.update { it.reducer() }
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
}
