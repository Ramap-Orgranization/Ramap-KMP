package com.peto.ramap.ui.review.write.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_visibility_description
import ramap.shared.generated.resources.review_visibility_title

@Composable
internal fun ReviewVisibilitySetting(
    state: ReviewWriteUiState,
    onIntent: (ReviewWriteIntent) -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = GrayColor.C050) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(text = stringResource(Res.string.review_visibility_title), style = AppTextStyle.B1, color = GrayColor.C500)
                AppText(text = stringResource(Res.string.review_visibility_description), style = AppTextStyle.C1, color = GrayColor.C400)
            }
            Switch(
                checked = state.isPublic,
                onCheckedChange = { onIntent(ReviewWriteIntent.ChangeVisibility(it)) },
                enabled = state.canEdit,
                colors = SwitchDefaults.colors(checkedThumbColor = CommonColor.White, checkedTrackColor = GrayColor.C500),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewVisibilityPrivatePreview() {
    RamapTheme {
        Column {
            ReviewVisibilitySetting(
                state = ReviewWriteUiState(isPublic = true),
                onIntent = {},
            )

            ReviewVisibilitySetting(
                state = ReviewWriteUiState(isPublic = false),
                onIntent = {},
            )
        }
    }
}
