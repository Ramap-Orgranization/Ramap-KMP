package com.peto.ramap.designsystem.text

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography

@Composable
fun AppMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    errorContent: @Composable (Modifier) -> Unit = { errorModifier ->
        AppText(
            text = content,
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            modifier = errorModifier,
        )
    },
) {
    val typography = LocalAppTypography.current
    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(text = GrayColor.C500),
        padding = markdownPadding(block = 8.dp),
        typography =
            markdownTypography(
                h1 = typography.h1,
                h2 = typography.h2,
                h3 = typography.t2,
                h4 = typography.b1,
                h5 = typography.t1,
                h6 = typography.t2,
                text = typography.b2,
                paragraph = typography.b2,
                quote = typography.b2,
                ordered = typography.b2,
                bullet = typography.b2,
                list = typography.b2,
                textLink =
                    TextLinkStyles(
                        SpanStyle(
                            color = GrayColor.C500,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
            ),
        imageTransformer = Coil3ImageTransformerImpl,
        loading = { loadingModifier ->
            Box(
                modifier = loadingModifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GrayColor.C500)
            }
        },
        error = errorContent,
    )
}
