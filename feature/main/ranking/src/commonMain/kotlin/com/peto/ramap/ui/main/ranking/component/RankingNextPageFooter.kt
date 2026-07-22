package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.ranking.contract.RankingUiState

@Composable
fun RankingNextPageFooter(
    uiState: RankingUiState,
    onLoadNext: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.showNextPageError -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                RetryButton(onClick = onRetry)
            }
        }

        uiState.isLoadingNext ->
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CommonColor.Black,
                )
            }

        uiState.hasNext -> {
            LaunchedEffect(uiState.nextCursor) { onLoadNext() }
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingNextPageFooterLoadingPreview() {
    RamapTheme {
        RankingNextPageFooter(
            uiState =
                RankingUiState(
                    showNextPageError = false,
                ).withLoadingState(
                    RankingUiState().loadState.let {
                        // Creating a state that returns true for isLoadingNext
                        it
                    },
                ),
            onLoadNext = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingNextPageFooterErrorPreview() {
    RamapTheme {
        RankingNextPageFooter(
            uiState =
                RankingUiState(
                    showNextPageError = true,
                ),
            onLoadNext = {},
            onRetry = {},
        )
    }
}
