package com.peto.ramap.ui.profile.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.ProfileColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.profile_bio
import ramap.shared.generated.resources.profile_bio_counter
import ramap.shared.generated.resources.profile_bio_invalid
import ramap.shared.generated.resources.profile_bio_placeholder
import ramap.shared.generated.resources.profile_complete
import ramap.shared.generated.resources.profile_counter
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_discard_body
import ramap.shared.generated.resources.profile_discard_title
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_keep_editing
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_nickname
import ramap.shared.generated.resources.profile_nickname_available
import ramap.shared.generated.resources.profile_nickname_check
import ramap.shared.generated.resources.profile_nickname_check_failed
import ramap.shared.generated.resources.profile_nickname_clear
import ramap.shared.generated.resources.profile_nickname_invalid
import ramap.shared.generated.resources.profile_nickname_placeholder
import ramap.shared.generated.resources.profile_nickname_unavailable
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_photo_change
import ramap.shared.generated.resources.profile_retry

@Composable
internal fun ProfileEditContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onLoginClick: (LoginType) -> Unit,
    onPickImage: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .safeDrawingPadding()
                .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onIntent(ProfileIntent.Back) }) {
                Image(
                    painter = painterResource(Res.drawable.ic_arrow3_left),
                    contentDescription = stringResource(Res.string.navigation_back),
                    modifier = Modifier.size(24.dp),
                )
            }
            AppText(
                text = stringResource(Res.string.profile_edit),
                style = AppTextStyle.T1,
                color = ProfileColor.Ink,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = { onIntent(ProfileIntent.Save) },
                enabled = state.canSave,
                modifier = Modifier.width(64.dp),
            ) {
                AppText(
                    text = stringResource(Res.string.profile_complete),
                    style = AppTextStyle.B1,
                    color = if (state.canSave) ProfileColor.Orange else ProfileColor.Muted,
                )
            }
        }
        BoxWithConstraints(
            modifier =
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            val contentHeight = maxHeight
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (state.loading) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.verticalScroll(rememberScrollState())
                            },
                        ),
            ) {
                when {
                    state.loading -> {
                        RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
                    }

                    state.userId == null -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            supportedLoginTypes().forEach { type ->
                                LoginButton(
                                    type = type,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onLoginClick(type) },
                                )
                            }
                        }
                    }

                    state.failed || state.profile == null -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppText(
                                text = stringResource(Res.string.profile_load_failed),
                                style = AppTextStyle.C1,
                                color = ProfileColor.Muted,
                            )
                            AppButton(
                                text = stringResource(Res.string.profile_retry),
                                onClick = { onIntent(ProfileIntent.Retry) },
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                backgroundColor = ProfileColor.Ink,
                            )
                        }
                    }

                    else -> {
                        ProfileEdit(
                            state = state,
                            onIntent = onIntent,
                            onPickImage = onPickImage,
                            contentHeight = contentHeight,
                        )
                    }
                }
            }
            if (state.checkingNickname || state.saving) {
                RamenLoadingIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(120.dp),
                )
            }
        }
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { onIntent(ProfileIntent.KeepEditing) },
            title = {
                AppText(
                    text = stringResource(Res.string.profile_discard_title),
                    style = AppTextStyle.T2,
                    color = ProfileColor.Ink,
                )
            },
            text = {
                AppText(
                    text = stringResource(Res.string.profile_discard_body),
                    style = AppTextStyle.B2,
                    color = ProfileColor.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(ProfileIntent.Discard) }) {
                    AppText(
                        text = stringResource(Res.string.profile_discard),
                        style = AppTextStyle.B1,
                        color = ProfileColor.Orange,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ProfileIntent.KeepEditing) }) {
                    AppText(
                        text = stringResource(Res.string.profile_keep_editing),
                        style = AppTextStyle.B2,
                        color = ProfileColor.Muted,
                    )
                }
            },
            containerColor = CommonColor.White,
        )
    }
}

@Composable
private fun ProfileEdit(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onPickImage: () -> Unit,
    contentHeight: Dp,
) {
    val nicknameLabel = stringResource(Res.string.profile_nickname)
    val bioLabel = stringResource(Res.string.profile_bio)
    val photo: Any? = state.image?.bytes ?: if (state.removePhoto) null else state.profile?.avatarUrl

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = contentHeight)
                .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ProfileAvatar(
                    model = photo,
                    description = stringResource(Res.string.profile_photo),
                    modifier = Modifier.size(128.dp),
                )
                IconButton(
                    onClick = onPickImage,
                    enabled = !state.saving,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                CommonColor.White,
                                RoundedCornerShape(24.dp),
                            ),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_camera_add),
                        contentDescription = stringResource(Res.string.profile_photo_change),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            AppText(
                text = stringResource(Res.string.profile_nickname),
                style = AppTextStyle.B1,
                color = ProfileColor.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = { onIntent(ProfileIntent.ChangeNickname(it)) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = nicknameLabel },
                    enabled = !state.saving,
                    singleLine = true,
                    isError = state.nicknameTouched && (state.nicknameInvalid || state.nicknameAvailable == false),
                    placeholder = {
                        AppText(
                            text = stringResource(Res.string.profile_nickname_placeholder),
                            style = AppTextStyle.B2,
                            color = ProfileColor.Muted,
                        )
                    },
                    trailingIcon = {
                        if (state.nickname.isNotEmpty()) {
                            IconButton(onClick = { onIntent(ProfileIntent.ChangeNickname("")) }) {
                                Image(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = stringResource(Res.string.profile_nickname_clear),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfileColor.Orange,
                            unfocusedBorderColor = ProfileColor.Line,
                            focusedTextColor = ProfileColor.Ink,
                            unfocusedTextColor = ProfileColor.Ink,
                            focusedContainerColor = Color(0xFFF6F7F8),
                            unfocusedContainerColor = Color(0xFFF6F7F8),
                        ),
                )
                AppButton(
                    text = stringResource(Res.string.profile_nickname_check),
                    onClick = { onIntent(ProfileIntent.CheckNickname) },
                    modifier = Modifier.width(96.dp),
                    textStyle = AppTextStyle.B2,
                    enabled = state.nicknameChanged && !state.nicknameInvalid && !state.checkingNickname && !state.saving,
                    cornerRadius = 12.dp,
                )
            }
            AppText(
                text = stringResource(Res.string.profile_counter, state.nickname.trim().length),
                style = AppTextStyle.C2,
                color = ProfileColor.Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            if (state.nicknameTouched && state.nicknameInvalid) {
                AppText(
                    text = stringResource(Res.string.profile_nickname_invalid),
                    style = AppTextStyle.C2,
                    color = Color(0xFFBA3424),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                )
            }
            if (!state.nicknameInvalid && state.nicknameChanged && (state.nicknameAvailable != null || state.nicknameCheckFailed)) {
                AppText(
                    text =
                        stringResource(
                            when {
                                state.nicknameCheckFailed -> Res.string.profile_nickname_check_failed
                                state.nicknameAvailable == true -> Res.string.profile_nickname_available
                                else -> Res.string.profile_nickname_unavailable
                            },
                        ),
                    style = AppTextStyle.C2,
                    color = if (state.nicknameAvailable == true) ProfileColor.Muted else Color(0xFFBA3424),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            AppText(
                text = stringResource(Res.string.profile_bio),
                style = AppTextStyle.B1,
                color = ProfileColor.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.bio,
                onValueChange = { onIntent(ProfileIntent.ChangeBio(it)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .semantics { contentDescription = bioLabel },
                enabled = !state.saving,
                minLines = 3,
                isError = state.bioInvalid,
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.profile_bio_placeholder),
                        style = AppTextStyle.B2,
                        color = ProfileColor.Muted,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfileColor.Orange,
                        unfocusedBorderColor = ProfileColor.Line,
                        focusedTextColor = ProfileColor.Ink,
                        unfocusedTextColor = ProfileColor.Ink,
                        focusedContainerColor = Color(0xFFF6F7F8),
                        unfocusedContainerColor = Color(0xFFF6F7F8),
                    ),
            )
            AppText(
                text = stringResource(Res.string.profile_bio_counter, ProfileBio.length(state.bio.trim())),
                style = AppTextStyle.C2,
                color = ProfileColor.Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            if (state.bioInvalid) {
                AppText(
                    text = stringResource(Res.string.profile_bio_invalid),
                    style = AppTextStyle.C2,
                    color = Color(0xFFBA3424),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun ProfileEditPreview() {
    RamapTheme {
        ProfileEditContent(
            state =
                ProfileUiState(
                    userId = "preview",
                    profile =
                        AccountProfile(
                            userId = "preview",
                            nickname = "느긋한차슈",
                            bio = "오늘도 맛있는 한 그릇을 찾아서",
                        ),
                    loadState = LoadState(),
                    editing = true,
                    nickname = "새로운차슈",
                    nicknameAvailable = true,
                    bio = "오늘도 맛있는 한 그릇을 찾아서",
                ),
            onIntent = {},
            onLoginClick = {},
            onPickImage = {},
        )
    }
}
