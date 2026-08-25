package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_instagram_action

@Composable
fun EventDetailSourceButton(
    sourceUrl: String,
    onSourceLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppButton(
        text = stringResource(Res.string.event_instagram_action),
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            InstagramColor.Yellow,
                            InstagramColor.Orange,
                            InstagramColor.Pink,
                            InstagramColor.Purple,
                            InstagramColor.Blue,
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                ),
        backgroundColor = Color.Transparent,
        onClick = { onSourceLinkClick(sourceUrl) },
    )
}

@Preview(showBackground = true)
@Composable
private fun EventDetailSourceButtonPreview() {
    RamapTheme {
        EventDetailSourceButton(
            sourceUrl = "https://instagram.com",
            onSourceLinkClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
