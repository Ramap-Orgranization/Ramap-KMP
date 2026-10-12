package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_author_review_count
import ramap.shared.generated.resources.review_author_unknown

@Composable
fun ReviewCardHeader(
    review: Review,
    actions: ReviewCardActions,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .noRippleClickable(
                        enabled = actions.onOpenProfile != null && review.author.userId.isNotBlank() && !review.isBlocked,
                        role = Role.Button,
                    ) {
                        actions.onOpenProfile?.invoke(review.author.userId)
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                model = review.author.avatarUrl,
                description = review.author.nickname,
                modifier = Modifier.size(44.dp),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (actions.onShopClick != null) {
                                Modifier.noRippleClickable {
                                    actions.onShopClick.invoke()
                                }
                            } else {
                                Modifier
                            },
                        ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppText(
                    text =
                        review.author.nickname.ifBlank {
                            stringResource(Res.string.review_author_unknown)
                        },
                    style = AppTextStyle.T2,
                    color = GrayColor.C500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!review.isBlocked) {
                    AppText(
                        text = stringResource(Res.string.review_author_review_count, review.author.reviewCount),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                }
            }
        }
        ReviewCardOverflowMenu(
            review = review,
            actions = actions,
            actionsEnabled = actionsEnabled,
        )
    }
}
