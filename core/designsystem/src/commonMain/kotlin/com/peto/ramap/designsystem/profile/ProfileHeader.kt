package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_profile_edit
import ramap.shared.generated.resources.profile_bio_empty
import ramap.shared.generated.resources.profile_edit
import ramap.shared.generated.resources.profile_photo
import ramap.shared.generated.resources.profile_title

@Composable
fun ProfileHeader(
    nickname: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null,
    avatarBadge: (@Composable BoxScope.() -> Unit)? = {
        if (onProfileClick != null) {
            ProfileEditBadge()
        }
    },
    bioContent: (@Composable () -> Unit)? = null,
) {
    val profileEditDescription = stringResource(Res.string.profile_edit)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onProfileClick != null) {
                        Modifier
                            .semantics { contentDescription = profileEditDescription }
                            .noRippleClickable(
                                role = Role.Button,
                                onClick = onProfileClick,
                            )
                    } else {
                        Modifier
                    },
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                    model = avatarUrl,
                    description = stringResource(Res.string.profile_photo),
                    modifier = Modifier.size(128.dp),
                )
                avatarBadge?.invoke(this)
            }
        }
        AppText(
            text = nickname ?: stringResource(Res.string.profile_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        bioContent?.invoke()
    }
}

@Composable
fun ProfileHeader(
    profile: AccountProfile?,
    failed: Boolean = false,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ProfileHeader(
        nickname = profile?.nickname,
        avatarUrl = profile?.avatarUrl,
        modifier = modifier,
        onProfileClick = onProfileClick,
        bioContent = {
            ProfileBioText(
                bio = profile?.bio.orEmpty(),
                showEmptyBio = (profile != null) && !failed,
            )
        },
    )
}

@Composable
fun ProfileHeader(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    ProfileHeader(
        nickname = profile.nickname,
        avatarUrl = profile.avatarUrl,
        modifier = modifier,
        bioContent = {
            ProfileBioText(
                bio = profile.bio,
                showEmptyBio = false,
            )
        },
    )
}

@Composable
private fun BoxScope.ProfileEditBadge() {
    Box(
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 6.dp)
                .size(40.dp)
                .border(2.dp, CommonColor.White, CircleShape)
                .clip(CircleShape)
                .background(ChromaticColor.Orange400),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_profile_edit),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = CommonColor.White,
        )
    }
}

@Composable
private fun ProfileBioText(
    bio: String,
    showEmptyBio: Boolean = false,
) {
    if (bio.isNotBlank()) {
        AppText(
            text = bio,
            style = AppTextStyle.C1,
            color = GrayColor.C400,
            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
            textAlign = TextAlign.Center,
        )
    } else if (showEmptyBio) {
        AppText(
            text = stringResource(Res.string.profile_bio_empty),
            style = AppTextStyle.C1,
            color = GrayColor.C400,
            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyProfileHeaderPreview() {
    RamapTheme {
        ProfileHeader(
            profile = AccountProfile(userId = "preview", nickname = "느긋한차슈", bio = ""),
            failed = false,
            onProfileClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherProfileHeaderPreview() {
    RamapTheme {
        ProfileHeader(
            profile = PublicProfile(userId = "preview", nickname = "느긋한차슈", bio = "라멘을 좋아해요"),
        )
    }
}
