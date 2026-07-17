package com.peto.ramap.designsystem.toast

import com.peto.ramap.designsystem.toast.model.ToastData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ToastManager {
    private val _toasts =
        MutableSharedFlow<ToastData>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val toasts: SharedFlow<ToastData> = _toasts.asSharedFlow()

    suspend fun show(data: ToastData) {
        _toasts.emit(data)
    }

    fun tryShow(data: ToastData): Boolean = _toasts.tryEmit(data)
}
