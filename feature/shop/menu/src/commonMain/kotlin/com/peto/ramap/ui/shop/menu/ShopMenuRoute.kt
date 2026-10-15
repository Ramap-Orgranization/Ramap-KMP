package com.peto.ramap.ui.shop.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShopMenuRoute(
    shopId: String,
    onMenuSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopMenuViewModel = koinViewModel(),
) {
    ShopMenuRoute(
        shopId = shopId,
        viewModel = viewModel,
    ) { uiState, onRetry ->
        ShopMenuContent(
            uiState = uiState,
            onMenuSourceClick = onMenuSourceClick,
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}

@Composable
fun ShopMenuRoute(
    shopId: String,
    viewModel: ShopMenuViewModel = koinViewModel(),
    content: @Composable (ShopMenuUiState, onRetry: () -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, shopId) {
        viewModel.dispatch(ShopMenuIntent.OnShopSelected(shopId))
    }

    content(uiState) { viewModel.dispatch(ShopMenuIntent.OnRetry) }
}
