package com.peto.ramap.ui.review.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchSideEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewShopSearchRoute(
    onBack: () -> Unit,
    onWriteReview: (String) -> Unit,
    viewModel: ReviewShopSearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is ReviewShopSearchSideEffect.WriteReview -> onWriteReview(effect.shopId)
        }
    }
    ReviewShopSearchContent(state = state, onBack = onBack, onIntent = viewModel::dispatch)
}
