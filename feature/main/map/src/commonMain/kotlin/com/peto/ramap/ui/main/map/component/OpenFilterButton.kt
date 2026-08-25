package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_open
import ramap.shared.generated.resources.open_shops_toggle

@Composable
internal fun OpenFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(Res.string.open_shops_toggle)
    Surface(
        modifier =
            modifier
                .size(48.dp)
                .semantics {
                    this.contentDescription = contentDescription
                    selected = isActive
                }.shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    clip = false,
                ),
        color = if (isActive) GrayColor.C500 else CommonColor.White,
        shape = CircleShape,
        onClick = onClick,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_open),
            contentDescription = null,
            colorFilter =
                ColorFilter.tint(
                    if (isActive) CommonColor.White else GrayColor.C500,
                ),
        )
    }
}

@Composable
@Preview
private fun OpenFilterButtonPreview() {
    RamapTheme {
        OpenFilterButton(
            isActive = false,
            onClick = {},
        )
    }
}

@Composable
@Preview
private fun OpenFilterButtonActivePreview() {
    RamapTheme {
        OpenFilterButton(
            isActive = true,
            onClick = {},
        )
    }
}
