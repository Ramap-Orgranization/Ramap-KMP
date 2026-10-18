package com.peto.ramap.designsystem.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.CommonColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppFloatingActionButton(
    icon: DrawableResource,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = CommonColor.White,
    iconModifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    Surface(
        modifier = modifier.noRippleClickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 6.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = iconModifier,
                colorFilter = iconTint?.let { ColorFilter.tint(it) },
            )
        }
    }
}
