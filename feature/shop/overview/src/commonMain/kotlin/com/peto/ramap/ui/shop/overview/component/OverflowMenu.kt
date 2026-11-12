package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.ShopBookmarkIcon
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_notification_filled
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.ic_share
import ramap.shared.generated.resources.share_shop_action
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_detail_more_actions
import ramap.shared.generated.resources.shop_notification_action

@Composable
internal fun OverflowMenu(
    shopId: String,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    showNotificationActions: Boolean,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember(shopId) { mutableStateOf(false) }
    val moreActionsDescription = stringResource(Res.string.shop_detail_more_actions)

    Box(modifier = modifier) {
        Image(
            painter = painterResource(Res.drawable.ic_more_vert),
            contentDescription = moreActionsDescription,
            modifier =
                Modifier
                    .size(40.dp)
                    .noRippleClickable { isExpanded = true }
                    .padding(8.dp),
            colorFilter = ColorFilter.tint(GrayColor.C500),
        )

        AppDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.widthIn(min = 150.dp),
        ) {
            OverflowMenuItem(
                text = stringResource(Res.string.share_shop_action),
                leadingIcon = { OverflowMenuIcon(icon = Res.drawable.ic_share) },
                onClick = {
                    isExpanded = false
                    onShareClick()
                },
            )
            OverflowMenuItem(
                text = stringResource(Res.string.bookmarked_shops_toggle),
                leadingIcon = {
                    ShopBookmarkIcon(
                        isBookmarked = isBookmarked,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    isExpanded = false
                    onBookmarkClick()
                },
            )
            if (showNotificationActions) {
                OverflowMenuItem(
                    text = stringResource(Res.string.shop_notification_action),
                    leadingIcon = {
                        OverflowMenuIcon(
                            icon = if (isNotificationEnabled) Res.drawable.ic_notification_filled else Res.drawable.ic_notification,
                            isActive = isNotificationEnabled,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onNotificationClick()
                    },
                )
            }
            OverflowMenuItem(
                text = stringResource(Res.string.shop_detail_link_report),
                leadingIcon = { OverflowMenuIcon(icon = Res.drawable.ic_report) },
                onClick = {
                    isExpanded = false
                    onReportClick()
                },
            )
        }
    }
}
