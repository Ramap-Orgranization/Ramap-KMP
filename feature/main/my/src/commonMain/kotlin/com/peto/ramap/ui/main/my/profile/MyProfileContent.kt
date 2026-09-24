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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.profile.ProfileCheckStamp
import com.peto.ramap.designsystem.profile.ProfileDotField
import com.peto.ramap.designsystem.profile.ProfileFooterPixels
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.domain.model.profile.ProfileInstagram
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.profile_account
import ramap.shared.generated.resources.profile_bio
import ramap.shared.generated.resources.profile_bio_counter
import ramap.shared.generated.resources.profile_bio_invalid
import ramap.shared.generated.resources.profile_bio_placeholder
import ramap.shared.generated.resources.profile_cancel
import ramap.shared.generated.resources.profile_connected
import ramap.shared.generated.resources.profile_counter
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_discard_body
import ramap.shared.generated.resources.profile_discard_title
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_edit_action
import ramap.shared.generated.resources.profile_instagram
import ramap.shared.generated.resources.profile_instagram_handle
import ramap.shared.generated.resources.profile_instagram_invalid
import ramap.shared.generated.resources.profile_instagram_open
import ramap.shared.generated.resources.profile_instagram_placeholder
import ramap.shared.generated.resources.profile_keep_editing
import ramap.shared.generated.resources.profile_load_failed
import ramap.shared.generated.resources.profile_nickname
import ramap.shared.generated.resources.profile_nickname_invalid
import ramap.shared.generated.resources.profile_nickname_placeholder
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_photo_change
import ramap.shared.generated.resources.profile_photo_formats
import ramap.shared.generated.resources.profile_photo_remove
import ramap.shared.generated.resources.profile_photo_select
import ramap.shared.generated.resources.profile_retry
import ramap.shared.generated.resources.profile_save
import ramap.shared.generated.resources.profile_title

private val ProfilePaper = Color(0xFFFFFEFB)
private val ProfileInk = Color(0xFF252721)
private val ProfileMuted = Color(0xFF777A70)
private val ProfileOrange = Color(0xFFE95432)
private val ProfileLine = Color(0xFFDEDFD5)

@Composable
internal fun MyProfileContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onLoginClick: (LoginType) -> Unit,
    onPickImage: () -> Unit,
    onOpenInstagram: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ProfilePaper)
            .safeDrawingPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(64.dp)) {
                IconButton(onClick = { onIntent(ProfileIntent.Back) }) {
                    Image(painterResource(Res.drawable.ic_arrow3_left), stringResource(Res.string.navigation_back), Modifier.size(24.dp))
                }
            }
            Text(stringResource(if (state.editing) Res.string.profile_edit else Res.string.profile_title), Modifier.weight(1f).semantics { heading() }, textAlign = TextAlign.Center, color = ProfileInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.widthIn(min = 64.dp), contentAlignment = Alignment.CenterEnd) {
                if (!state.editing && !state.loading && !state.failed && state.userId != null && state.profile != null) {
                    TextButton(onClick = { onIntent(ProfileIntent.Edit) }) {
                        Text(stringResource(Res.string.profile_edit_action), color = ProfileOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f)) {
            val contentHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ProfileOrange) }
                    state.userId == null ->
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            supportedLoginTypes().forEach { type -> LoginButton(type = type, modifier = Modifier.fillMaxWidth(), onClick = { onLoginClick(type) }) }
                        }
                    state.failed || state.profile == null ->
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(Res.string.profile_load_failed), color = ProfileMuted)
                            ProfileButton(stringResource(Res.string.profile_retry), { onIntent(ProfileIntent.Retry) }, Modifier.padding(top = 20.dp))
                        }
                    state.editing -> ProfileEdit(state, onIntent, onPickImage, contentHeight)
                    else -> ProfileView(state, contentHeight, onOpenInstagram)
                }
            }
        }
    }
    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { onIntent(ProfileIntent.KeepEditing) },
            title = { Text(stringResource(Res.string.profile_discard_title)) },
            text = { Text(stringResource(Res.string.profile_discard_body)) },
            confirmButton = { TextButton(onClick = { onIntent(ProfileIntent.Discard) }) { Text(stringResource(Res.string.profile_discard)) } },
            dismissButton = { TextButton(onClick = { onIntent(ProfileIntent.KeepEditing) }) { Text(stringResource(Res.string.profile_keep_editing)) } },
            containerColor = ProfilePaper,
        )
    }
}

@Composable
private fun ProfileView(
    state: ProfileUiState,
    contentHeight: Dp,
    onOpenInstagram: (String) -> Unit,
) {
    val profile = state.profile ?: return
    Column(Modifier.fillMaxWidth().heightIn(min = contentHeight), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Box(Modifier.fillMaxWidth()) {
                ProfileDotField(Modifier.fillMaxWidth().height(156.dp).padding(horizontal = 12.dp))
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        ProfileAvatar(profile.avatarUrl, stringResource(Res.string.profile_photo), Modifier.size(128.dp))
                        ProfileCheckStamp(Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 4.dp).size(34.dp))
                    }
                    Text(profile.nickname, Modifier.padding(top = 23.dp, start = 24.dp, end = 24.dp), color = ProfileInk, fontSize = 29.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (profile.bio.isNotBlank()) {
                        Text(profile.bio, Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp), color = ProfileMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    val instagramUrl = ProfileInstagram.url(profile.instagramUsername)
                    if (instagramUrl != null) {
                        val description = stringResource(Res.string.profile_instagram_open, profile.instagramUsername)
                        TextButton(
                            onClick = { onOpenInstagram(instagramUrl) },
                            modifier = Modifier.padding(horizontal = 24.dp).semantics { contentDescription = description },
                        ) {
                            Image(painterResource(Res.drawable.instagram_icon), null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.profile_instagram_handle, profile.instagramUsername), color = ProfileOrange, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(Modifier.padding(horizontal = 24.dp)) {
                HorizontalDivider(color = ProfileLine)
                Text(stringResource(Res.string.profile_account), Modifier.padding(top = 23.dp, bottom = 17.dp), color = ProfileMuted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Image(painterResource(Res.drawable.ic_person), null, Modifier.size(36.dp).background(Color(0xFFF0F1EA), RoundedCornerShape(12.dp)).padding(8.dp))
                    Text(state.email ?: stringResource(Res.string.profile_account), Modifier.weight(1f), fontSize = 13.sp, color = ProfileInk)
                    Text(stringResource(Res.string.profile_connected), Modifier.background(Color(0xFFF0F1EA), RoundedCornerShape(4.dp)).padding(7.dp), color = ProfileMuted, fontSize = 10.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), contentAlignment = Alignment.CenterEnd) {
            ProfileFooterPixels(Modifier.size(width = 45.dp, height = 18.dp))
        }
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
    Column(Modifier.fillMaxWidth().heightIn(min = contentHeight).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ProfileAvatar(photo, stringResource(Res.string.profile_photo), Modifier.size(128.dp))
                IconButton(onClick = { photoDialog = true }, enabled = !state.saving, modifier = Modifier.size(48.dp).background(ProfilePaper, RoundedCornerShape(24.dp))) {
                    Image(painterResource(Res.drawable.ic_camera_add), stringResource(Res.string.profile_photo_change), Modifier.size(24.dp))
                }
            }
            TextButton(onClick = { photoDialog = true }, enabled = !state.saving) { Text(stringResource(Res.string.profile_photo_change), color = ProfileOrange, fontSize = 12.sp) }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.profile_nickname), color = ProfileInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.profile_counter, state.nickname.trim().length), color = ProfileMuted, fontSize = 11.sp)
            }
            OutlinedTextField(
                value = state.nickname,
                onValueChange = { onIntent(ProfileIntent.ChangeNickname(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).semantics { contentDescription = nicknameLabel },
                enabled = !state.saving,
                singleLine = true,
                isError = state.nicknameTouched && state.nicknameInvalid,
                placeholder = { Text(stringResource(Res.string.profile_nickname_placeholder), fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ProfileOrange, unfocusedBorderColor = ProfileLine, focusedTextColor = ProfileInk, unfocusedTextColor = ProfileInk),
            )
            if (state.nicknameTouched && state.nicknameInvalid) {
                Text(stringResource(Res.string.profile_nickname_invalid), Modifier.fillMaxWidth().padding(top = 10.dp), color = Color(0xFFBA3424), fontSize = 11.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.profile_bio), color = ProfileInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.profile_bio_counter, ProfileBio.length(state.bio.trim())), color = ProfileMuted, fontSize = 11.sp)
            }
            OutlinedTextField(
                value = state.bio,
                onValueChange = { onIntent(ProfileIntent.ChangeBio(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).semantics { contentDescription = bioLabel },
                enabled = !state.saving,
                singleLine = true,
                isError = state.bioInvalid,
                placeholder = { Text(stringResource(Res.string.profile_bio_placeholder), fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ProfileOrange, unfocusedBorderColor = ProfileLine, focusedTextColor = ProfileInk, unfocusedTextColor = ProfileInk),
            )
            if (state.bioInvalid) {
                Text(stringResource(Res.string.profile_bio_invalid), Modifier.fillMaxWidth().padding(top = 10.dp), color = Color(0xFFBA3424), fontSize = 11.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(instagramLabel, Modifier.fillMaxWidth(), color = ProfileInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.instagram,
                onValueChange = { onIntent(ProfileIntent.ChangeInstagram(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).semantics { contentDescription = instagramLabel },
                enabled = !state.saving,
                singleLine = true,
                isError = state.instagramInvalid,
                placeholder = { Text(stringResource(Res.string.profile_instagram_placeholder), fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ProfileOrange, unfocusedBorderColor = ProfileLine, focusedTextColor = ProfileInk, unfocusedTextColor = ProfileInk),
            )
            if (state.instagramInvalid) {
                Text(stringResource(Res.string.profile_instagram_invalid), Modifier.fillMaxWidth().padding(top = 10.dp), color = Color(0xFFBA3424), fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileButton(stringResource(Res.string.profile_save), { onIntent(ProfileIntent.Save) }, Modifier.padding(top = 36.dp), state.canSave)
            if (state.saving) CircularProgressIndicator(Modifier.padding(top = 12.dp).size(24.dp), color = ProfileOrange)
        }
    }
    if (photoDialog) {
        AlertDialog(
            onDismissRequest = { photoDialog = false },
            title = { Text(stringResource(Res.string.profile_photo)) },
            text = {
                Column {
                    TextButton(onClick = {
                        photoDialog = false
                        onPickImage()
                    }) { Text(stringResource(Res.string.profile_photo_select)) }
                    TextButton(onClick = {
                        photoDialog = false
                        onIntent(ProfileIntent.RemovePhoto)
                    }, enabled = photo != null) { Text(stringResource(Res.string.profile_photo_remove)) }
                    Text(stringResource(Res.string.profile_photo_formats), fontSize = 11.sp, color = ProfileMuted)
                }
            },
            confirmButton = { TextButton(onClick = { photoDialog = false }) { Text(stringResource(Res.string.profile_cancel)) } },
            containerColor = ProfilePaper,
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
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = ProfileInk, contentColor = Color.White, disabledContainerColor = Color(0xFFE5E6DF), disabledContentColor = Color(0xFF8B8E82))) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
fun MyProfilePreview() {
    RamapTheme { MyProfileContent(ProfileUiState(userId = "preview", profile = AccountProfile("preview", "느긋한차슈", bio = "오늘도 맛있는 한 그릇을 찾아서", instagramUsername = "ramap_official"), email = "ramen@example.com", loading = false), {}, {}, {}, {}) }
}

@Preview
@Composable
fun MyProfileEditPreview() {
    RamapTheme { MyProfileContent(ProfileUiState(userId = "preview", profile = AccountProfile("preview", "느긋한차슈", bio = "오늘도 맛있는 한 그릇을 찾아서", instagramUsername = "ramap_official"), loading = false, editing = true, nickname = "느긋한차슈", bio = "오늘도 맛있는 한 그릇을 찾아서", instagram = "ramap_official"), {}, {}, {}, {}) }
}
