package com.peto.ramap.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class ReviewChangeNotifier {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = changes.asSharedFlow()

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}
