package com.peto.ramap.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.navigation_back

@Composable
fun SettingsPage(
    title: StringResource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CommonTopBar(
                title = stringResource(title),
                left = {
                    Image(
                        painter = painterResource(Res.drawable.ic_arrow3_left),
                        contentDescription = stringResource(Res.string.navigation_back),
                        modifier =
                            Modifier
                                .padding(18.dp)
                                .size(24.dp)
                                .noRippleClickable(onClick = onBack),
                    )
                },
            )
            Column { content() }
        }

        if (showLoading) {
            RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview
@Composable
private fun SettingsPagePreview() {
    RamapTheme {
        SettingsPage(title = Res.string.navigation_back, onBack = {}, showLoading = true) {
        }
    }
}
