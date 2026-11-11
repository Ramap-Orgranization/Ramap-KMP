package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_reviews_visibility
import ramap.shared.generated.resources.follow_saved_shops_visibility
import ramap.shared.generated.resources.profile_discard
import ramap.shared.generated.resources.profile_visibility
import ramap.shared.generated.resources.profile_visibility_public
import ramap.shared.generated.resources.review_save

@Composable
internal fun ProfileVisibilityDialog(
    visible: Boolean,
    profile: AccountProfile?,
    savingVisibility: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProfileVisibility) -> Unit,
) {
    if (!visible) return

    val isPublic = profile?.isPublic == true
    var draftPublic by remember(visible, profile) { mutableStateOf(isPublic) }
    var draftReviews by remember(visible, profile) {
        mutableStateOf(profile?.followersCanReadReviews ?: true)
    }
    var draftSavedShops by remember(visible, profile) {
        mutableStateOf(profile?.followersCanReadSavedShops ?: true)
    }

    CommonDialog(
        visible = visible,
        confirmText = stringResource(Res.string.review_save),
        dismissText = stringResource(Res.string.profile_discard),
        confirmEnabled = !savingVisibility,
        onDismissRequest = onDismiss,
        onConfirm = {
            onSave(ProfileVisibility(draftPublic, draftReviews, draftSavedShops))
        },
        onDismiss = onDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.profile_visibility),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            VisibilitySwitch(
                label = stringResource(Res.string.profile_visibility_public),
                checked = draftPublic,
                enabled = !savingVisibility,
                onCheckedChange = { draftPublic = it },
            )
            VisibilitySwitch(
                label = stringResource(Res.string.follow_reviews_visibility),
                checked = draftReviews,
                enabled = !savingVisibility,
                onCheckedChange = { draftReviews = it },
            )
            VisibilitySwitch(
                label = stringResource(Res.string.follow_saved_shops_visibility),
                checked = draftSavedShops,
                enabled = !savingVisibility,
                onCheckedChange = { draftSavedShops = it },
            )
        },
    )
}

@Composable
private fun VisibilitySwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(
            text = label,
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileVisibilityDialogPreview() {
    RamapTheme {
        ProfileVisibilityDialog(
            visible = true,
            profile =
                AccountProfile(
                    userId = "preview",
                    nickname = "느긋한차슈",
                    isPublic = true,
                    followersCanReadReviews = true,
                    followersCanReadSavedShops = false,
                ),
            savingVisibility = false,
            onDismiss = {},
            onSave = {},
        )
    }
}
