package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.operating_notice_saved

@Composable
internal fun SavedFilterButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(48.dp)
                .semantics { this.selected = selected }
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    clip = false,
                ),
        shape = CircleShape,
        color = if (selected) GrayColor.C500 else CommonColor.White,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_kid_star),
                contentDescription = stringResource(Res.string.operating_notice_saved),
                tint = if (selected) CommonColor.White else GrayColor.C500,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedFilterButtonPreview() {
    RamapTheme {
        SavedFilterButton(
            selected = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedFilterButtonSelectedPreview() {
    RamapTheme {
        SavedFilterButton(
            selected = true,
            onClick = {},
        )
    }
}
