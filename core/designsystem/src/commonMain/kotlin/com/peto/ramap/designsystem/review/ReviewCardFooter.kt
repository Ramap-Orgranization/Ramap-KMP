package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_visit_number

@Composable
fun ReviewCardFooter(
    review: Review,
    onLike: (() -> Unit)?,
    isLikeLoading: Boolean,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!review.isBlocked) {
            ReviewLikeCount(
                count = review.likeCount,
                isLiked = review.isLiked,
                modifier =
                    if (onLike != null) {
                        Modifier.noRippleClickable(
                            enabled = !isLikeLoading && actionsEnabled,
                            onClick = onLike,
                        )
                    } else {
                        Modifier
                    },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            AppText(
                text = reviewDateLabel(review.createdAt),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
            )
            AppText(
                text = " · ",
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            AppText(
                text =
                    stringResource(
                        Res.string.review_visit_number,
                        review.visitNumber,
                    ),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
            )
        }
    }
}
