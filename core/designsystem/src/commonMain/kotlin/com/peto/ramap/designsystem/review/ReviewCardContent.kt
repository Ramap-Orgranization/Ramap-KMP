package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_review_private
import ramap.shared.generated.resources.review_blocked_review
import ramap.shared.generated.resources.review_collapse
import ramap.shared.generated.resources.review_more
import ramap.shared.generated.resources.review_private_status
import ramap.shared.generated.resources.review_status_removed

@Composable
fun ReviewCardContent(
    review: Review,
    currentUserId: String?,
    currentProfileIsPublic: Boolean?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(review.id, review.body) { mutableStateOf(false) }
    var hasOverflow by remember(review.id, review.body) { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (review.isBlocked) {
            AppText(
                text = stringResource(Res.string.review_blocked_review),
                style = AppTextStyle.C1,
                color = GrayColor.C300,
            )
        }
        AppText(
            text = review.body,
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                if (!expanded) hasOverflow = layout.hasVisualOverflow
            },
        )
        if (hasOverflow) {
            AppText(
                text = stringResource(if (expanded) Res.string.review_collapse else Res.string.review_more),
                modifier = Modifier.clickable(role = Role.Button) { expanded = !expanded },
                style = AppTextStyle.C1,
                color = GrayColor.C300,
            )
        }
        if (review.moderationStatus == ReviewModerationStatus.REMOVED) {
            AppText(
                text = stringResource(Res.string.review_status_removed),
                color = GrayColor.C300,
                style = AppTextStyle.C1,
            )
        }
        if ((!review.isPublic || currentProfileIsPublic == false) &&
            !currentUserId.isNullOrBlank() &&
            review.author.userId == currentUserId
        ) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GrayColor.C050)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_review_private),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(GrayColor.C300),
                )
                AppText(
                    text = stringResource(Res.string.review_private_status),
                    style = AppTextStyle.C1,
                    color = GrayColor.C300,
                )
            }
        }
    }
}
