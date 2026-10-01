package com.peto.ramap.ui.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

@Composable
fun ObserveLoadMoreNearListEnd(
    listState: LazyListState,
    itemThreshold: Int,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    require(itemThreshold > 0)

    val currentHasMore = rememberUpdatedState(hasMore)
    val currentIsLoading = rememberUpdatedState(isLoading)
    val currentOnLoadMore = rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState, itemThreshold) {
        var canRequestAfterLeavingEnd = true
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val isNearEnd =
                layoutInfo.totalItemsCount > 0 &&
                    lastVisibleIndex >= layoutInfo.totalItemsCount - itemThreshold
            isNearEnd to (currentHasMore.value && !currentIsLoading.value)
        }.collect { (isNearEnd, canLoadMore) ->
            if (!isNearEnd) {
                canRequestAfterLeavingEnd = true
            } else if (canRequestAfterLeavingEnd && canLoadMore) {
                canRequestAfterLeavingEnd = false
                currentOnLoadMore.value()
            }
        }
    }
}
