package com.peto.ramap.ui.bookmark.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.designsystem.component.ShopListCount
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.paging.InfiniteLazyColumn
import com.peto.ramap.ui.paging.InfiniteScrollState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_confirm_action
import ramap.shared.generated.resources.bookmarked_shops_empty_title
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.ic_add
import ramap.shared.generated.resources.importation_add_action_description
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.settings_bookmarked_shops_menu

@Composable
fun BookmarkedShopListRoute(
    onBack: () -> Unit,
    onImportationNavigate: () -> Unit,
    onShopOpen: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: BookmarkedShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is BookmarkedShopListSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    BookmarkedShopListScreen(
        uiState = uiState,
        onBack = onBack,
        onImportationNavigate = onImportationNavigate,
        onShopOpen = { onShopOpen(it.id) },
        onRemovalRequested = { shop ->
            viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed(shop.id))
        },
        onRetry = { viewModel.dispatch(BookmarkedShopListIntent.OnRetry) },
        onLoadMore = { viewModel.dispatch(BookmarkedShopListIntent.LoadMore) },
    )
}

@Composable
internal fun BookmarkedShopListScreen(
    uiState: BookmarkedShopListUiState,
    onBack: () -> Unit,
    onImportationNavigate: () -> Unit,
    onShopOpen: (RamenShop) -> Unit,
    onRemovalRequested: (RamenShop) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.settings_bookmarked_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
        topBarAction = {
            IconButton(onClick = onImportationNavigate, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.importation_add_action_description),
                    modifier = Modifier.size(24.dp),
                    tint = GrayColor.C500,
                )
            }
        },
    ) {
        BookmarkedShopListContent(
            uiState = uiState,
            onShopOpen = onShopOpen,
            onRemovalRequested = onRemovalRequested,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
        )
    }
}

@Composable
private fun BookmarkedShopListContent(
    uiState: BookmarkedShopListUiState,
    onShopOpen: (RamenShop) -> Unit,
    onRemovalRequested: (RamenShop) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    if (uiState.shops.isEmpty()) {
        ShopsEmptyContent(
            title = stringResource(Res.string.bookmarked_shops_empty_title),
        )
        return
    }

    val shops = remember(uiState.shops) { uiState.shops.values.toList() }
    val shopIds = remember(shops) { shops.map(RamenShop::id) }
    val removalLabel = stringResource(Res.string.bookmark_removal_confirm_action)
    InfiniteLazyColumn(
        loadMoreState =
            InfiniteScrollState(
                itemCount = shops.size,
                hasMore = uiState.hasMore,
                isLoading = uiState.isPageLoading,
                itemKeys = shopIds,
                // 조회 결과가 비어 있어도 다음 페이지 요청을 허용한다.
                requestKey = uiState.remainingShopCount,
            ),
        onLoadMore = onLoadMore,
        loadMoreEnabled = !uiState.appendFailed,
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ShopListCount(count = uiState.shopCount) }
        itemsIndexed(items = shops, key = { _, shop -> shop.id }) { index, shop ->
            RamenShopSummary(
                shop = shop,
                onClick = { onShopOpen(shop) },
                actionLabel = removalLabel,
                onAction = { onRemovalRequested(shop) },
                modifier =
                    Modifier
                        .padding(horizontal = 15.dp)
                        .padding(bottom = if (index == shops.lastIndex) 0.dp else 12.dp),
            )
        }
        if (uiState.isPageLoading) {
            item { RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().height(60.dp)) }
        }
        if (uiState.appendFailed) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    RetryButton(onClick = onRetry)
                }
            }
        }
    }
}
