package com.peto.ramap.ui.profile.review.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.review.ReviewPixelHeader
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.profile.review.contract.ProfileReviewUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_block
import ramap.shared.generated.resources.review_report_user
import ramap.shared.generated.resources.review_unblock

@Composable
internal fun ProfileHeader(
    state: ProfileReviewUiState,
    onReport: () -> Unit,
    onBlock: (PublicProfile) -> Unit,
) {
    val profile = state.profile ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileAvatar(
            model = profile.avatarUrl,
            description = profile.nickname,
            modifier = Modifier.size(88.dp),
        )
        ReviewPixelHeader(
            title = profile.nickname,
            description = profile.bio,
        )
        if (profile.userId != state.currentUserId) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppButton(
                    text = stringResource(if (state.isBlocked) Res.string.review_unblock else Res.string.review_block),
                    onClick = { onBlock(profile) },
                    modifier = Modifier.weight(1f),
                    textStyle = AppTextStyle.B3,
                    textColor = GrayColor.C500,
                    backgroundColor = CommonColor.White,
                    border = BorderStroke(1.dp, GrayColor.C500),
                    cornerRadius = 0.dp,
                )
                AppButton(
                    text = stringResource(Res.string.review_report_user),
                    onClick = onReport,
                    modifier = Modifier.weight(1f),
                    textStyle = AppTextStyle.B3,
                    textColor = GrayColor.C500,
                    backgroundColor = CommonColor.White,
                    cornerRadius = 0.dp,
                )
            }
        }
        HorizontalDivider(color = GrayColor.C100)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    RamapTheme {
        ProfileHeader(
            state =
                ProfileReviewUiState(
                    profile =
                        PublicProfile(
                            userId = "user-1",
                            nickname = "면발수집",
                            bio = "맛있는 라멘을 찾아 전국을 떠돌아다닙니다.",
                            avatarUrl = null,
                        ),
                    currentUserId = "user-2",
                ),
            onReport = {},
            onBlock = {},
        )
    }
}
