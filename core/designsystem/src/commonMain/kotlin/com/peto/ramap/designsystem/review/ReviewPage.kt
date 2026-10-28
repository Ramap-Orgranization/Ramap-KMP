package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.topbar.BackTopBar

@Composable
fun ReviewPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
        ) {
            BackTopBar(
                onBack = onBack,
                title = title,
                action = action,
            )
            content()
        }
    }
}
