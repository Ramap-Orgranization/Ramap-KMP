package com.peto.ramap.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.peto.ramap.designsystem.image.LocalImageAccessScope
import com.peto.ramap.domain.repository.ImageAccessRepository

@Composable
internal fun ImageAccessProvider(
    access: ImageAccessRepository,
    content: @Composable () -> Unit,
) {
    val scope by access.scopes.collectAsState(initial = access.snapshot())
    val context = LocalPlatformContext.current
    LaunchedEffect(scope) {
        val cache = SingletonImageLoader.get(context).memoryCache
        val prefix = scope.memoryKey("")
        cache?.keys?.filter { it.key.startsWith("private:") && !it.key.startsWith(prefix) }?.forEach(cache::remove)
    }
    CompositionLocalProvider(LocalImageAccessScope provides scope, content = content)
}
