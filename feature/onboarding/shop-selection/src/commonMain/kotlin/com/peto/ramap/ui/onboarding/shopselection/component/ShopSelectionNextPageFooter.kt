package com.peto.ramap.ui.onboarding.shopselection.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionUiState

@Composable
internal fun ShopSelectionNextPageFooter(
    uiState: ShopSelectionUiState,
    onLoadNext: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.hasNextPageError -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                RetryButton(onClick = onRetry)
            }
        }

        uiState.isLoadingNextPage -> ShopSelectionSkeleton(rowCount = 1)

        uiState.canLoadNextPage -> {
            LaunchedEffect(uiState.nextCursor) { onLoadNext() }
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
