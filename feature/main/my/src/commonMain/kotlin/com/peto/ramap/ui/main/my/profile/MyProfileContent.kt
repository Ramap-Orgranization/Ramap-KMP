package com.peto.ramap.ui.main.my.profile

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.theme.AppTextStyle
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
import ramap.shared.generated.resources.profile_cancel
import ramap.shared.generated.resources.profile_complete
import ramap.shared.generated.resources.profile_counter
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_discard_body
import ramap.shared.generated.resources.profile_discard_title
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_instagram
import ramap.shared.generated.resources.profile_instagram_invalid
import ramap.shared.generated.resources.profile_instagram_placeholder
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
import ramap.shared.generated.resources.profile_photo_formats
import ramap.shared.generated.resources.profile_photo_remove
import ramap.shared.generated.resources.profile_photo_select
import ramap.shared.generated.resources.profile_retry

@Composable
internal fun MyProfileContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onLoginClick: (LoginType) -> Unit,
    onPickImage: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ProfileColor.Paper)
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
                            ProfileButton(
                                text = stringResource(Res.string.profile_retry),
                                onClick = { onIntent(ProfileIntent.Retry) },
                                modifier = Modifier.padding(top = 20.dp),
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
                            .size(120.dp)
                            .background(ProfileColor.Paper.copy(alpha = 0.9f), RoundedCornerShape(16.dp)),
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
            containerColor = ProfileColor.Paper,
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
    var photoDialog by remember(state.draftGeneration) { mutableStateOf(false) }
    val nicknameLabel = stringResource(Res.string.profile_nickname)
    val bioLabel = stringResource(Res.string.profile_bio)
    val instagramLabel = stringResource(Res.string.profile_instagram)
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
                    onClick = { photoDialog = true },
                    enabled = !state.saving,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(ProfileColor.Paper, RoundedCornerShape(24.dp)),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_camera_add),
                        contentDescription = stringResource(Res.string.profile_photo_change),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            TextButton(
                onClick = { photoDialog = true },
                enabled = !state.saving,
            ) {
                AppText(
                    text = stringResource(Res.string.profile_photo_change),
                    style = AppTextStyle.B4,
                    color = ProfileColor.Orange,
                )
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
                Button(
                    onClick = { onIntent(ProfileIntent.CheckNickname) },
                    enabled = state.nicknameChanged && !state.nicknameInvalid && !state.checkingNickname && !state.saving,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE9E3), contentColor = ProfileColor.Orange),
                ) {
                    AppText(
                        text = stringResource(Res.string.profile_nickname_check),
                        style = AppTextStyle.B2,
                        color = ProfileColor.Orange,
                    )
                }
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            Spacer(modifier = Modifier.height(24.dp))
            AppText(
                text = instagramLabel,
                style = AppTextStyle.B1,
                color = ProfileColor.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.instagram,
                onValueChange = { onIntent(ProfileIntent.ChangeInstagram(it)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .semantics { contentDescription = instagramLabel },
                enabled = !state.saving,
                singleLine = true,
                isError = state.instagramInvalid,
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.profile_instagram_placeholder),
                        style = AppTextStyle.B2,
                        color = ProfileColor.Muted,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                shape = RoundedCornerShape(8.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfileColor.Orange,
                        unfocusedBorderColor = ProfileColor.Line,
                        focusedTextColor = ProfileColor.Ink,
                        unfocusedTextColor = ProfileColor.Ink,
                    ),
            )
            if (state.instagramInvalid) {
                AppText(
                    text = stringResource(Res.string.profile_instagram_invalid),
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
    if (photoDialog) {
        AlertDialog(
            onDismissRequest = { photoDialog = false },
            title = {
                AppText(
                    text = stringResource(Res.string.profile_photo),
                    style = AppTextStyle.T2,
                    color = ProfileColor.Ink,
                )
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            photoDialog = false
                            onPickImage()
                        },
                    ) {
                        AppText(
                            text = stringResource(Res.string.profile_photo_select),
                            style = AppTextStyle.B2,
                            color = ProfileColor.Ink,
                        )
                    }
                    TextButton(
                        onClick = {
                            photoDialog = false
                            onIntent(ProfileIntent.RemovePhoto)
                        },
                        enabled = photo != null,
                    ) {
                        AppText(
                            text = stringResource(Res.string.profile_photo_remove),
                            style = AppTextStyle.B2,
                            color = if (photo != null) ProfileColor.Orange else ProfileColor.Muted,
                        )
                    }
                    AppText(
                        text = stringResource(Res.string.profile_photo_formats),
                        style = AppTextStyle.C2,
                        color = ProfileColor.Muted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { photoDialog = false }) {
                    AppText(
                        text = stringResource(Res.string.profile_cancel),
                        style = AppTextStyle.B2,
                        color = ProfileColor.Muted,
                    )
                }
            },
            containerColor = ProfileColor.Paper,
        )
    }
}

@Composable
private fun ProfileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = ProfileColor.Ink,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE5E6DF),
                disabledContentColor = Color(0xFF8B8E82),
            ),
    ) {
        AppText(
            text = text,
            style = AppTextStyle.B1,
            color = if (enabled) Color.White else Color(0xFF8B8E82),
        )
    }
}

@Preview
@Composable
fun MyProfilePreview() {
    RamapTheme {
        MyProfileContent(
            state =
                ProfileUiState(
                    userId = "preview",
                    profile =
                        AccountProfile(
                            userId = "preview",
                            nickname = "느긋한차슈",
                            bio = "오늘도 맛있는 한 그릇을 찾아서",
                            instagramUsername = "ramap_official",
                        ),
                    loadState = LoadState(),
                    editing = true,
                    nickname = "새로운차슈",
                    nicknameAvailable = true,
                    bio = "오늘도 맛있는 한 그릇을 찾아서",
                    instagram = "ramap_official",
                ),
            onIntent = {},
            onLoginClick = {},
            onPickImage = {},
        )
    }
}
