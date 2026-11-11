package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(Res.string.profile_load_failed),
            style = AppTextStyle.B3,
            color = GrayColor.C400,
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
