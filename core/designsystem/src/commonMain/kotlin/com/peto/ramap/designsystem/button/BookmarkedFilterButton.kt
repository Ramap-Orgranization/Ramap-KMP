package com.peto.ramap.designsystem.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.RecommendationColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_star_filled

@Composable
fun BookmarkedFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.bookmarked_shops_toggle),
) {
    AppCircleIconButton(
        icon = Res.drawable.ic_star_filled,
        isActive = isActive,
        contentDescription = contentDescription,
        selected = isActive,
        onClick = onClick,
        modifier = modifier,
        tint = if (isActive) RecommendationColor.Brown else GrayColor.C500,
        activeColor = RecommendationColor.Gold,
        activeBorder = BorderStroke(2.dp, RecommendationColor.Brown),
    )
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
