package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.review_blocked_options
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_edit
import ramap.shared.generated.resources.review_report
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.shop_detail_more_actions

@Composable
fun ReviewCardOverflowMenu(
    review: Review,
    actions: ReviewCardActions,
    actionsEnabled: Boolean,
) {
    var menuExpanded by remember(review.id) { mutableStateOf(false) }
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
