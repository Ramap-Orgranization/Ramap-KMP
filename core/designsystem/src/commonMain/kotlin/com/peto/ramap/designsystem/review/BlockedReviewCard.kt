package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_review_lock
import ramap.shared.generated.resources.review_blocked_options
import ramap.shared.generated.resources.review_blocked_review
import ramap.shared.generated.resources.review_unblock_user
import ramap.shared.generated.resources.review_view_blocked_once

@Composable
fun BlockedReviewCard(
    modifier: Modifier = Modifier,
    onViewReview: (() -> Unit)? = null,
    onUnblockUser: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CommonColor.White),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayColor.C050)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(CommonColor.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_review_lock),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(GrayColor.C300),
                )
            }
            AppText(
                text = stringResource(Res.string.review_blocked_review),
                style = AppTextStyle.B1,
                color = GrayColor.C400,
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = GrayColor.C300,
                    strokeWidth = 2.dp,
                )
            } else if (onViewReview != null || onUnblockUser != null) {
                Box {
                    Image(
                        painter = painterResource(Res.drawable.ic_more_vert),
                        contentDescription = stringResource(Res.string.review_blocked_options),
                        modifier =
                            Modifier
                                .size(20.dp)
                                .noRippleClickable(role = Role.Button) { menuExpanded = true },
                        colorFilter = ColorFilter.tint(GrayColor.C500),
                    )
                    AppDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onViewReview != null) {
                            AppDropdownMenuItem(
                                text = stringResource(Res.string.review_view_blocked_once),
                                onClick = {
                                    menuExpanded = false
                                    onViewReview()
                                },
                            )
                        }
                        if (onUnblockUser != null) {
                            AppDropdownMenuItem(
                                text = stringResource(Res.string.review_unblock_user),
                                onClick = {
                                    menuExpanded = false
                                    onUnblockUser()
                                },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = GrayColor.C100)
    }
}
