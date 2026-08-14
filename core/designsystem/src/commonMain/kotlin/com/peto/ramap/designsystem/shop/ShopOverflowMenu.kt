package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.event_notification_action
import ramap.shared.generated.resources.hide_shop_action
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_notification_filled
import ramap.shared.generated.resources.ic_share
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.share_shop_action
import ramap.shared.generated.resources.shop_detail_more_actions

@Composable
internal fun ShopOverflowMenu(
    shopId: String,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    showNotificationActions: Boolean,
    isHidden: Boolean,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
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

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.widthIn(min = 150.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = CommonColor.White,
        ) {
            ShopOverflowMenuItem(
                text = stringResource(Res.string.share_shop_action),
                icon = Res.drawable.ic_share,
                isActive = null,
                onClick = {
                    isExpanded = false
                    onShareClick()
                },
            )
            if (!isHidden) {
                ShopOverflowMenuItem(
                    text = stringResource(Res.string.bookmarked_shops_toggle),
                    icon = if (isBookmarked) Res.drawable.ic_kid_star_filled else Res.drawable.ic_kid_star,
                    isActive = isBookmarked,
                    onClick = {
                        onBookmarkClick()
                    },
                )
                if (showNotificationActions) {
                    ShopOverflowMenuItem(
                        text = stringResource(Res.string.event_notification_action),
                        icon =
                            if (isNotificationEnabled) {
                                Res.drawable.ic_notification_filled
                            } else {
                                Res.drawable.ic_notification
                            },
                        isActive = isNotificationEnabled,
                        onClick = onNotificationClick,
                    )
                }
            }
            ShopOverflowMenuItem(
                text = stringResource(Res.string.hide_shop_action),
                icon = Res.drawable.ic_visibility_off,
                isActive = isHidden,
                onClick = {
                    isExpanded = false
                    onHiddenClick()
                },
            )
        }
    }
}
