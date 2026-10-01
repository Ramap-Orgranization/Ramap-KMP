package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_blocked_on
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.review_blocked_users_empty
import ramap.shared.generated.resources.review_close
import ramap.shared.generated.resources.review_unblock

@Composable
internal fun BlockedUsersDialog(
    visible: Boolean,
    users: List<BlockedUser>,
    onDismiss: () -> Unit,
    onProfileClick: (String) -> Unit,
    onUnblockClick: (String) -> Unit,
) {
    if (!visible) return
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_close),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        content = {
            BlockedUsersContent(
                users = users,
                title = stringResource(Res.string.review_blocked_users),
                emptyMessage = stringResource(Res.string.review_blocked_users_empty),
                unblockLabel = stringResource(Res.string.review_unblock),
                onProfileClick = onProfileClick,
                onUnblockClick = onUnblockClick,
            )
        },
    )
}

@Composable
private fun BlockedUsersContent(
    users: List<BlockedUser>,
    title: String,
    emptyMessage: String,
    unblockLabel: String,
    onProfileClick: (String) -> Unit,
    onUnblockClick: (String) -> Unit,
) {
    AppText(
        text = title,
        style = AppTextStyle.T1,
        color = GrayColor.C500,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (users.isEmpty()) {
        AppText(
            text = emptyMessage,
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
    ) {
        itemsIndexed(users, key = { _, user -> user.profile.userId }) { index, user ->
            BlockedUserRow(
                user = user,
                unblockLabel = unblockLabel,
                onProfileClick = onProfileClick,
                onUnblockClick = onUnblockClick,
            )
            if (index < users.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(GrayColor.C100))
            }
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUser,
    unblockLabel: String,
    onProfileClick: (String) -> Unit,
    onUnblockClick: (String) -> Unit,
) {
    val profile = user.profile
    val blockedDate = user.blockedOn.toString().replace('-', '.')
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable { onProfileClick(profile.userId) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                model = profile.avatarUrl,
                description = profile.nickname,
                modifier = Modifier.width(48.dp).height(48.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = profile.nickname,
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppText(
                    text = stringResource(Res.string.review_blocked_on, blockedDate),
                    style = AppTextStyle.B4,
                    color = GrayColor.C300,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppButton(
            text = unblockLabel,
            onClick = { onUnblockClick(profile.userId) },
            modifier = Modifier.width(76.dp),
            textColor = GrayColor.C400,
            textStyle = AppTextStyle.B4,
            backgroundColor = CommonColor.White,
            border = BorderStroke(1.dp, GrayColor.C200),
        )
    }
}

@Preview
@Composable
private fun BlockedUsersDialogPreview() {
    RamapTheme {
        BlockedUsersDialog(
            visible = true,
            users =
                listOf(
                    BlockedUser(
                        profile =
                            PublicProfile(
                                userId = "1",
                                nickname = "라멘마니아",
                                avatarUrl = null,
                            ),
                        blockedOn = LocalDate(2024, 1, 15),
                    ),
                    BlockedUser(
                        profile =
                            PublicProfile(
                                userId = "2",
                                nickname = "차슈더해줘",
                                avatarUrl = null,
                            ),
                        blockedOn = LocalDate(2024, 2, 20),
                    ),
                ),
            onDismiss = {},
            onProfileClick = {},
            onUnblockClick = {},
        )
    }
}
