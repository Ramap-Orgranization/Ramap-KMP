package com.peto.ramap.ui.profile.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.peto.ramap.designsystem.profile.ProfileDotField
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.loading.LoadState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_info
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.profile_bio_counter
import ramap.shared.generated.resources.profile_bio_invalid
import ramap.shared.generated.resources.profile_bio_placeholder
import ramap.shared.generated.resources.profile_bio_short
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
import ramap.shared.generated.resources.profile_nickname_placeholder
import ramap.shared.generated.resources.profile_nickname_unavailable
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_photo_change
import ramap.shared.generated.resources.profile_photo_remove
import ramap.shared.generated.resources.profile_policy_bullet_1
import ramap.shared.generated.resources.profile_policy_bullet_2
import ramap.shared.generated.resources.profile_policy_bullet_3
import ramap.shared.generated.resources.profile_policy_title
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
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
                textAlign = TextAlign.Center,
            )
            Box(
                modifier =
                    Modifier
                        .width(64.dp)
                        .heightIn(min = 48.dp)
                        .noRippleClickable(enabled = state.canSave) {
                            onIntent(ProfileIntent.Save)
                        },
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = stringResource(Res.string.profile_complete),
                    style = AppTextStyle.B1,
                    color = if (state.canSave) ChromaticColor.Orange400 else GrayColor.C300,
                    textAlign = TextAlign.Center,
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
                                color = GrayColor.C300,
                            )
                            AppButton(
                                text = stringResource(Res.string.profile_retry),
                                onClick = { onIntent(ProfileIntent.Retry) },
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                backgroundColor = GrayColor.C500,
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
                    color = GrayColor.C500,
                )
            },
            text = {
                AppText(
                    text = stringResource(Res.string.profile_discard_body),
                    style = AppTextStyle.B2,
                    color = GrayColor.C300,
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(ProfileIntent.Discard) }) {
                    AppText(
                        text = stringResource(Res.string.profile_discard),
                        style = AppTextStyle.B1,
                        color = ChromaticColor.Orange400,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ProfileIntent.KeepEditing) }) {
                    AppText(
                        text = stringResource(Res.string.profile_keep_editing),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
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
    val bioLabel = stringResource(Res.string.profile_bio_short)
    val photo: Any? = state.image?.bytes ?: if (state.removePhoto) null else state.profile?.avatarUrl

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = contentHeight)
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                ProfileDotField(modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.size(128.dp)) {
                    ProfileAvatar(
                        model = photo,
                        description = stringResource(Res.string.profile_photo),
                        modifier = Modifier.size(128.dp),
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .size(40.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .background(ChromaticColor.Orange400)
                                .noRippleClickable(enabled = !state.saving, onClick = onPickImage),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera_add),
                            contentDescription = stringResource(Res.string.profile_photo_change),
                            modifier = Modifier.size(26.dp),
                            tint = Color.White,
                        )
                    }
                    if (photo != null) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .noRippleClickable(
                                        enabled = !state.saving,
                                        onClick = { onIntent(ProfileIntent.RemovePhoto) },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = stringResource(Res.string.profile_photo_remove),
                                modifier = Modifier.size(18.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            AppText(
                text = stringResource(Res.string.profile_nickname),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = { onIntent(ProfileIntent.ChangeNickname(it)) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { contentDescription = nicknameLabel },
                    enabled = !state.saving,
                    singleLine = true,
                    isError = state.nicknameTouched && (state.nicknameInvalid || state.nicknameAvailable == false),
                    placeholder = {
                        AppText(
                            text = stringResource(Res.string.profile_nickname_placeholder),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    },
                    trailingIcon = {
                        if (state.nickname.isNotEmpty()) {
                            IconButton(
                                enabled = !state.saving,
                                onClick = { onIntent(ProfileIntent.ChangeNickname("")) },
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = stringResource(Res.string.profile_nickname_clear),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.showNicknameStatus) {
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
                                        color = if (state.nicknameAvailable == true) GrayColor.C300 else SystemColor.Warning,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText(
                                text = stringResource(Res.string.profile_counter, state.nickname.trim().length),
                                style = AppTextStyle.C2,
                                color = GrayColor.C300,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChromaticColor.Orange400,
                            unfocusedBorderColor = CommonColor.White,
                            focusedTextColor = GrayColor.C500,
                            unfocusedTextColor = GrayColor.C500,
                            focusedContainerColor = GrayColor.C050,
                            unfocusedContainerColor = GrayColor.C050,
                        ),
                )
                AppButton(
                    text = stringResource(Res.string.profile_nickname_check),
                    onClick = { onIntent(ProfileIntent.CheckNickname) },
                    modifier = Modifier.width(60.dp),
                    textStyle = AppTextStyle.B3,
                    enabled = state.canCheckNickname,
                    cornerRadius = 12.dp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            AppText(
                text = stringResource(Res.string.profile_bio_short),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
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
                        color = GrayColor.C300,
                    )
                },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (state.bioInvalid) {
                                AppText(
                                    text = stringResource(Res.string.profile_bio_invalid),
                                    style = AppTextStyle.C2,
                                    color = SystemColor.Warning,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AppText(
                            text = stringResource(Res.string.profile_bio_counter, ProfileBio.length(state.bio.trim())),
                            style = AppTextStyle.C2,
                            color = GrayColor.C300,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChromaticColor.Orange400,
                        unfocusedBorderColor = CommonColor.White,
                        focusedTextColor = GrayColor.C500,
                        unfocusedTextColor = GrayColor.C500,
                        focusedContainerColor = GrayColor.C050,
                        unfocusedContainerColor = GrayColor.C050,
                    ),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GrayColor.C050,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = GrayColor.C500,
                        )
                        AppText(
                            text = stringResource(Res.string.profile_policy_title),
                            style = AppTextStyle.B1,
                            color = GrayColor.C500,
                        )
                    }
                    val bulletStyle = AppTextStyle.B4
                    val bulletColor = GrayColor.C400
                    AppText(
                        text = "• " + stringResource(Res.string.profile_policy_bullet_1),
                        style = bulletStyle,
                        color = bulletColor,
                    )
                    AppText(
                        text = "• " + stringResource(Res.string.profile_policy_bullet_2),
                        style = bulletStyle,
                        color = bulletColor,
                    )
                    AppText(
                        text = "• " + stringResource(Res.string.profile_policy_bullet_3),
                        style = bulletStyle,
                        color = bulletColor,
                    )
                }
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
