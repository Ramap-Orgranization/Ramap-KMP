package com.peto.ramap.ui.announcement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.announcement.contract.AnnouncementIntent
import com.peto.ramap.ui.refresh.RefreshOnReturnEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnnouncementRoute(
    onBack: () -> Unit,
    viewModel: AnnouncementViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshOnReturnEffect { viewModel.dispatch(AnnouncementIntent.ReturnToScreen) }
    AnnouncementContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::dispatch,
    )
}
