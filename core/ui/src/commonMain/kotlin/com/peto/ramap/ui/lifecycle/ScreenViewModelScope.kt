package com.peto.ramap.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
fun ScreenViewModelScope(
    screenKey: Any?,
    content: @Composable () -> Unit,
) {
    key(screenKey) {
        val owner = remember { CompositionViewModelStoreOwner() }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
}
