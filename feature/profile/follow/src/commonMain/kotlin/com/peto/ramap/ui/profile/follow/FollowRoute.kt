package com.peto.ramap.ui.profile.follow

import com.peto.ramap.ui.profile.follow.contract.FollowIntent
import com.peto.ramap.ui.profile.follow.contract.FollowState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.refresh.RefreshOnReturnEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_update_failure_message

@Composable
fun FollowRoute(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    isUncovered: Boolean = true,
    viewModel: FollowViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var refreshOnReturn by rememberSaveable { mutableStateOf(false) }
    val openProfile: (String) -> Unit = { userId ->
        refreshOnReturn = true
        onOpenProfile(userId)
    }
    RefreshOnReturnEffect(isUncovered = isUncovered) {
        if (refreshOnReturn) {
            refreshOnReturn = false
            viewModel.dispatch(FollowIntent.ReturnedFromProfile)
        }
    }
    ObserveAsEvents(viewModel.sideEffect) {
        toastManager.show(ToastData(message = Res.string.follow_update_failure_message, type = ToastType.ERROR))
    }
    FollowContent(
        state = state,
        onBack = onBack,
        onOpenProfile = openProfile,
        onIntent = viewModel::dispatch,
    )
}
