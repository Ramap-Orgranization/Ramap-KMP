package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_kid_star

@Composable
internal fun BookmarkedFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapCircleIconButton(
        isActive = isActive,
        onClick = onClick,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_kid_star),
            contentDescription = stringResource(Res.string.bookmarked_shops_toggle),
            colorFilter =
                ColorFilter.tint(
                    if (isActive) CommonColor.White else GrayColor.C500,
                ),
        )
    }
}

@Composable
@Preview
private fun BookmarkedFilterButtonPreview() {
    RamapTheme {
        BookmarkedFilterButton(
            isActive = false,
            onClick = {},
        )
    }
}

@Composable
@Preview
private fun BookmarkedFilterButtonActivePreview() {
    RamapTheme {
        BookmarkedFilterButton(
            isActive = true,
            onClick = {},
        )
    }
}
