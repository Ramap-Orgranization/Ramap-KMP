package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
internal fun MapCircleIconButton(
    isActive: Boolean,
    contentDescription: String,
    selected: Boolean? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
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
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}
