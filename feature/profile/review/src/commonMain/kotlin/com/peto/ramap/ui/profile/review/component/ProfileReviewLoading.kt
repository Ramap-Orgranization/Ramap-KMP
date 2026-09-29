package com.peto.ramap.ui.profile.review.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_loading

@Composable
internal fun FeedLoading() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RamenLoadingIndicator(modifier = Modifier.size(120.dp))
        AppText(
            text = stringResource(Res.string.review_loading),
            modifier = Modifier.padding(start = 12.dp),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedLoadingPreview() {
    RamapTheme {
        FeedLoading()
    }
}
