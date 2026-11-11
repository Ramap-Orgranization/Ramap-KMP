package com.peto.ramap.ui.review.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.lifecycle.ScreenViewModelScope
import com.peto.ramap.ui.review.detail.component.ReviewCommentThreadContent
import com.peto.ramap.ui.review.detail.component.ReviewCommentsFooter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReviewCommentsPreviewRoute(reviewId: String) {
    ScreenViewModelScope(reviewId) {
        val viewModel = koinViewModel<ReviewDetailViewModel> { parametersOf(reviewId, false) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            state.threads.forEach { thread ->
                ReviewCommentThreadContent(thread, state, viewModel::dispatch, showReplyAction = false)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ReviewCommentsFooter(state, viewModel::dispatch)
            }
        }
    }
}
