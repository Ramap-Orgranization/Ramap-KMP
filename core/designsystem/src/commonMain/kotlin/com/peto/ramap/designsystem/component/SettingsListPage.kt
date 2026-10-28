package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.topbar.BackTopBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsListPage(
    title: StringResource,
    onBack: () -> Unit,
    showError: Boolean,
    showInitialLoading: Boolean,
    showOverlayLoading: Boolean,
    errorImage: DrawableResource,
    errorDescription: StringResource,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    topBarAction: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackTopBar(
            onBack = onBack,
            title = stringResource(title),
            action = topBarAction,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                showError ->
                    LoadErrorContent(
                        image = errorImage,
                        title = stringResource(title),
                        description = stringResource(errorDescription),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )

                showInitialLoading -> ShopListSkeleton(modifier = Modifier.fillMaxSize())
                else -> content()
            }

            if (showOverlayLoading && !showInitialLoading && !showError) {
                RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
