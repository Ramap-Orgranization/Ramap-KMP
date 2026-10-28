package com.peto.ramap.designsystem.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GrayColor.C400,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
