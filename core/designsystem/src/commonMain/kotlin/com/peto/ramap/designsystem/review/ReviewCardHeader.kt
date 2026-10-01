package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.review_author_review_count
import ramap.shared.generated.resources.review_author_unknown
import ramap.shared.generated.resources.review_blocked_options
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_edit
import ramap.shared.generated.resources.review_report
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.shop_detail_more_actions

@Composable
fun ReviewCardHeader(
    review: Review,
    actions: ReviewCardActions,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember(review.id) { mutableStateOf(false) }

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
        if (
            actions.onEdit != null ||
            actions.onDelete != null ||
            actions.onReport != null ||
            (review.isBlocked && actions.onUnblockBlockedUser != null)
        ) {
            Box {
                Image(
                    painter = painterResource(Res.drawable.ic_more_vert),
                    contentDescription =
                        stringResource(
                            if (review.isBlocked) Res.string.review_blocked_options else Res.string.shop_detail_more_actions,
                        ),
                    modifier =
                        Modifier
                            .size(20.dp)
                            .noRippleClickable(
                                enabled = actionsEnabled,
                                role = Role.Button,
                            ) {
                                menuExpanded = true
                            },
                    colorFilter = ColorFilter.tint(GrayColor.C500),
                )
                AppDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    actions.onEdit?.let { onEdit ->
                        AppDropdownMenuItem(
                            text = stringResource(Res.string.review_edit),
                            enabled = actionsEnabled,
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                    }
                    actions.onDelete?.let { onDelete ->
                        AppDropdownMenuItem(
                            text = stringResource(Res.string.review_delete),
                            enabled = actionsEnabled,
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                    actions.onReport?.let { onReport ->
                        AppDropdownMenuItem(
                            text = stringResource(Res.string.review_report),
                            enabled = actionsEnabled,
                            onClick = {
                                menuExpanded = false
                                onReport()
                            },
                        )
                    }
                    if (review.isBlocked) {
                        actions.onUnblockBlockedUser?.let { onUnblock ->
                            AppDropdownMenuItem(
                                text = stringResource(Res.string.review_unblock_user),
                                onClick = {
                                    menuExpanded = false
                                    onUnblock()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
