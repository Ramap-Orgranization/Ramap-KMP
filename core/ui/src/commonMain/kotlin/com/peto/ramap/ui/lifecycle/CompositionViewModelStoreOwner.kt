package com.peto.ramap.ui.lifecycle

import androidx.compose.runtime.RememberObserver
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** 화면의 Composition이 끝나면 ViewModel의 작업과 구독을 함께 해제한다. */
class CompositionViewModelStoreOwner :
    ViewModelStoreOwner,
    RememberObserver {
    override val viewModelStore = ViewModelStore()

    override fun onRemembered() = Unit

    override fun onForgotten() = viewModelStore.clear()

    override fun onAbandoned() = viewModelStore.clear()
}
