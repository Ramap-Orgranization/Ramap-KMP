package com.peto.ramap.ui.main.my.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.image.rememberImagePicker
import com.peto.ramap.ui.base.ObserveAsEvents
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyProfileRoute(
    onBack: () -> Unit,
    onLoginClick: (LoginType) -> Unit,
    viewModel: MyProfileViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickerGeneration by remember { mutableStateOf(-1L) }
    val pickImage =
        rememberImagePicker(
            onImagePicked = { viewModel.dispatch(ProfileIntent.PickImage(ProfileImage(it.bytes, it.mimeType), pickerGeneration)) },
            onRejected = { viewModel.dispatch(ProfileIntent.RejectImage) },
        )
    DisposableEffect(viewModel) {
        onDispose { viewModel.dispatch(ProfileIntent.Leave) }
    }
    ObserveAsEvents(viewModel.sideEffect) {
        when (it) {
            ProfileSideEffect.NavigateBack -> onBack()
            is ProfileSideEffect.Toast -> toastManager.show(ToastData(it.message, ToastType.DEFAULT))
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None),
        isBackEnabled = state.editing,
        onBackCompleted = { viewModel.dispatch(ProfileIntent.Back) },
    )
    MyProfileContent(
        state = state,
        onIntent = viewModel::dispatch,
        onLoginClick = onLoginClick,
        onOpenInstagram = ExternalUriOpener::open,
        onPickImage = {
            pickerGeneration = state.draftGeneration
            pickImage()
        },
    )
}
