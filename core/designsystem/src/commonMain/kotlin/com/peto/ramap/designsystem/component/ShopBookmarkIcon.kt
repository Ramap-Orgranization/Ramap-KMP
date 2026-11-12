package com.peto.ramap.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_star_filled
import ramap.shared.generated.resources.ic_star_outline

@Composable
fun ShopBookmarkIcon(
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(24.dp)) {
        if (isBookmarked) {
            Image(
                painter = painterResource(Res.drawable.ic_star_filled),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(BrandColor.Fill),
            )
        }
        Image(
            painter = painterResource(Res.drawable.ic_star_outline),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            colorFilter = ColorFilter.tint(if (isBookmarked) BrandColor.Foreground else GrayColor.C300),
        )
    }
}
