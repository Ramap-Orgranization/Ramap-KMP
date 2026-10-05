package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_heart_filled
import ramap.shared.generated.resources.ic_heart_outline
import ramap.shared.generated.resources.review_like
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
            Row(
                modifier =
                    if (onLike != null) {
                        Modifier.noRippleClickable(
                            enabled = !isLikeLoading && actionsEnabled,
                            onClick = onLike,
                        )
                    } else {
                        Modifier
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(if (review.isLiked) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline),
                    contentDescription = stringResource(Res.string.review_like, review.likeCount),
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(if (review.isLiked) SystemColor.Warning else GrayColor.C300),
                )
                AppText(
                    text = review.likeCount.toString(),
                    style = AppTextStyle.B2,
                    color = GrayColor.C500,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            AppText(
                text = review.createdAt.take(10).replace('-', '.'),
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
