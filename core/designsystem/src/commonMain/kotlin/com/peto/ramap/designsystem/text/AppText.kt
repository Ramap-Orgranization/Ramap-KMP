package com.peto.ramap.designsystem.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.toTextStyle

@Composable
fun AppText(
    text: String,
    style: AppTextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val typography = LocalAppTypography.current
    val baseStyle = style.toTextStyle(typography)

    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Text(
            text = text,
            modifier = modifier,
            style = baseStyle,
            color = color,
            textAlign = textAlign,
            textDecoration = textDecoration,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
