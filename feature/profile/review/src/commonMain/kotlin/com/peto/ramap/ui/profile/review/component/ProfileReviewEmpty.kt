package com.peto.ramap.ui.profile.review.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.profile.review.contract.ProfileReviewUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_blocked_empty
import ramap.shared.generated.resources.review_profile_unavailable

@Composable
internal fun FeedEmpty(
    state: ProfileReviewUiState,
) {
    Surface(
        color = GrayColor.C050,
        border = BorderStroke(1.dp, GrayColor.C100),
        shape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppText(
                text =
                    stringResource(
                        if (state.isBlocked) Res.string.review_blocked_empty else Res.string.review_profile_unavailable,
                    ),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedEmptyPreview() {
    RamapTheme {
        FeedEmpty(state = ProfileReviewUiState())
    }
}
