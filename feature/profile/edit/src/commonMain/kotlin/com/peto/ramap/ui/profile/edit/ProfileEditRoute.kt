package com.peto.ramap.ui.profile.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.platform.image.rememberImagePicker
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.loading.LoadState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileEditRoute(
    onBack: () -> Unit,
    onLoginClick: (LoginType) -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel(),
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
    ProfileEditRouteContent(
        state = state,
        onIntent = viewModel::dispatch,
        onLoginClick = onLoginClick,
        onPickImage = {
            if (!state.saving) {
                pickerGeneration = state.draftGeneration
                pickImage()
            }
        },
    )
}

@Composable
internal fun ProfileEditRouteContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onLoginClick: (LoginType) -> Unit,
    onPickImage: () -> Unit = {},
) {
    NavigationBackHandler(
        state = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None),
        isBackEnabled = state.editing,
        onBackCompleted = { onIntent(ProfileIntent.Back) },
    )
    ProfileEditContent(
        state = state,
        onIntent = onIntent,
        onLoginClick = onLoginClick,
        onPickImage = onPickImage,
    )
}

@Preview
@Composable
private fun ProfileEditRoutePreview() {
    RamapTheme {
        ProfileEditRouteContent(
            state =
                ProfileUiState(
                    userId = "preview",
                    profile =
                        AccountProfile(
                            userId = "preview",
                            nickname = "느긋한차슈",
                            bio = "오늘도 맛있는 한 그릇을 찾아서",
                        ),
                    email = "ramen@example.com",
                    loadState = LoadState(),
                    editing = true,
                    nickname = "느긋한차슈",
                    bio = "오늘도 맛있는 한 그릇을 찾아서",
                ),
            onIntent = {},
            onLoginClick = {},
        )
    }
}
