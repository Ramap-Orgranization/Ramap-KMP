package com.peto.ramap.designsystem.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppCircleIconButton(
    icon: DrawableResource,
    isActive: Boolean,
    contentDescription: String,
    selected: Boolean? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    isLoading: Boolean = false,
) {
    Surface(
        modifier =
            modifier
                .size(48.dp)
                .semantics {
                    this.contentDescription = contentDescription
                    selected?.let { this.selected = it }
                }.shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    clip = false,
                ),
        color = if (isActive) GrayColor.C500 else CommonColor.White,
        shape = CircleShape,
        onClick = { if (!isLoading) onClick() },
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = GrayColor.C500,
                    strokeWidth = 2.dp,
                )
            } else {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = tint?.let { ColorFilter.tint(it) },
                )
            }
        }
    }
}
