package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.review_blocked_users_empty
import ramap.shared.generated.resources.review_close

@Composable
internal fun BlockedUsersDialog(
    visible: Boolean,
    users: List<PublicProfile>,
    onDismiss: () -> Unit,
    onProfileClick: (String) -> Unit,
) {
    if (!visible) return
    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_close),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.review_blocked_users),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (users.isEmpty()) {
                AppText(
                    text = stringResource(Res.string.review_blocked_users_empty),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                ) {
                    items(users, key = { it.userId }) { profile ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfileClick(profile.userId) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppText(
                                text = profile.nickname,
                                style = AppTextStyle.B1,
                                color = GrayColor.C500,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BlockedUsersDialogPreview() {
    RamapTheme {
        BlockedUsersDialog(
            visible = true,
            users = emptyList(),
            onDismiss = {},
            onProfileClick = {},
        )
    }
}
