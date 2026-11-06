package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_star_filled

@Composable
internal fun ShopBookmarkButton(
    isBookmarked: Boolean,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bookmarkDescription = stringResource(Res.string.bookmarked_shops_toggle)

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .toggleable(
                    value = isBookmarked,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                ).semantics { contentDescription = bookmarkDescription },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_star_filled),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(if (isBookmarked) InstagramColor.Pink else GrayColor.C300),
        )
    }
}
