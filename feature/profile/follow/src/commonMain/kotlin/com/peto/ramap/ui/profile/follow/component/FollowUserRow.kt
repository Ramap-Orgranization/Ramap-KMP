package com.peto.ramap.ui.profile.follow.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_approve
import ramap.shared.generated.resources.follow_reject
import ramap.shared.generated.resources.follow_remove

@Composable
internal fun FollowUserRow(
    profile: PublicProfile,
    list: FollowList,
    enabled: Boolean,
    actingAction: FollowAction?,
    onOpenProfile: () -> Unit,
    onRemove: () -> Unit,
    onAction: (FollowAction) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable(enabled = enabled, onClick = onOpenProfile).padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(model = profile.avatarUrl, description = profile.nickname, modifier = Modifier.size(48.dp))
                AppText(
                    text = profile.nickname,
                    style = AppTextStyle.T2,
                    color = GrayColor.C500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            when (list) {
                FollowList.FOLLOWERS ->
                    AppButton(
                        text = stringResource(Res.string.follow_remove),
                        onClick = onRemove,
                        enabled = enabled,
                        textStyle = AppTextStyle.C1,
                        textColor = GrayColor.C300,
                        backgroundColor = CommonColor.White,
                        border = BorderStroke(1.dp, GrayColor.C100),
                        cornerRadius = 22.dp,
                        height = 44.dp,
                        modifier = Modifier.width(94.dp),
                    )
                FollowList.REQUESTS ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        AppButton(
                            text = stringResource(Res.string.follow_approve),
                            onClick = { onAction(FollowAction.APPROVE) },
                            enabled = enabled,
                            isLoading = actingAction == FollowAction.APPROVE,
                            textStyle = AppTextStyle.C1,
                            backgroundColor = ChromaticColor.Orange400,
                            cornerRadius = 22.dp,
                            height = 44.dp,
                            modifier = Modifier.width(48.dp),
                        )
                        AppButton(
                            text = stringResource(Res.string.follow_reject),
                            onClick = { onAction(FollowAction.REJECT) },
                            enabled = enabled,
                            isLoading = actingAction == FollowAction.REJECT,
                            textStyle = AppTextStyle.C1,
                            textColor = GrayColor.C300,
                            backgroundColor = CommonColor.White,
                            border = BorderStroke(1.dp, GrayColor.C100),
                            cornerRadius = 22.dp,
                            height = 44.dp,
                            modifier = Modifier.width(48.dp),
                        )
                    }
                FollowList.FOLLOWING -> Unit
            }
        }
        HorizontalDivider(color = GrayColor.C100)
    }
}
