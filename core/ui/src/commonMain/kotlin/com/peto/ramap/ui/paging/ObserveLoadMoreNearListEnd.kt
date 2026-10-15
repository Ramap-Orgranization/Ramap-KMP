package com.peto.ramap.ui.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

@Composable
fun ObserveLoadMoreNearListEnd(
    listState: LazyListState,
    itemThreshold: Int,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    ObserveLoadMoreNearListEnd(
        listState = listState,
        itemThreshold = itemThreshold,
        // 데이터 개수를 전달하지 않는 기존 호출은 끝 영역 재진입 시에만 다시 요청한다.
        loadMoreState = InfiniteScrollState(itemCount = 0, hasMore = hasMore, isLoading = isLoading),
        onLoadMore = onLoadMore,
    )
}

@Composable
fun ObserveLoadMoreNearListEnd(
    listState: LazyListState,
    loadMoreState: InfiniteScrollState,
    itemThreshold: Int = 3,
    isEnabled: Boolean = true,
    onLoadMore: () -> Unit,
) {
    require(itemThreshold > 0)
    require(loadMoreState.itemCount >= 0)

    val triggerKeys =
        remember(loadMoreState.itemKeys, itemThreshold) {
            loadMoreState.itemKeys?.takeLast(itemThreshold)?.toSet()
        }
    val isNearEnd by remember(listState, triggerKeys, itemThreshold) {
        derivedStateOf {
            val layout = listState.layoutInfo
            if (triggerKeys != null) {
                layout.visibleItemsInfo.any { it.key in triggerKeys }
            } else {
                val lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
                layout.totalItemsCount > 0 && lastVisibleIndex >= layout.totalItemsCount - itemThreshold
            }
        }
    }
    var requestedItemCount by remember(listState, loadMoreState.requestKey) { mutableStateOf<Int?>(null) }
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val canLoadMore = isEnabled && loadMoreState.hasMore && !loadMoreState.isLoading && !loadMoreState.isRefreshing

    LaunchedEffect(listState, loadMoreState.requestKey, isEnabled, isNearEnd, loadMoreState.isRefreshing, loadMoreState.itemCount, canLoadMore) {
        if (!isEnabled || !isNearEnd || loadMoreState.isRefreshing) {
            requestedItemCount = null
            return@LaunchedEffect
        }
        if (canLoadMore && requestedItemCount != loadMoreState.itemCount) {
            requestedItemCount = loadMoreState.itemCount
            currentOnLoadMore()
        }
    }
}
