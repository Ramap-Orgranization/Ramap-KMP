package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_load_failed

@Composable
internal fun ProfileLoadFailedBanner(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = stringResource(Res.string.profile_load_failed),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        RetryButton(
            onClick = onRetryClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileLoadFailedBannerPreview() {
    RamapTheme {
        ProfileLoadFailedBanner(
            onRetryClick = {},
        )
    }
}
