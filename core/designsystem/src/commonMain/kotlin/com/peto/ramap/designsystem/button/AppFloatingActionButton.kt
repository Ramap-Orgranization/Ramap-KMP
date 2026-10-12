package com.peto.ramap.designsystem.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = containerColor,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = iconModifier,
            colorFilter = iconTint?.let { ColorFilter.tint(it) },
        )
    }
}
