package com.peto.ramap.designsystem.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ic_kid_star

@Composable
fun BookmarkedFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.bookmarked_shops_toggle),
) {
    AppCircleIconButton(
        icon = Res.drawable.ic_kid_star,
        isActive = isActive,
        contentDescription = contentDescription,
        selected = isActive,
        onClick = onClick,
        modifier = modifier,
        tint = if (isActive) CommonColor.White else GrayColor.C500,
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
