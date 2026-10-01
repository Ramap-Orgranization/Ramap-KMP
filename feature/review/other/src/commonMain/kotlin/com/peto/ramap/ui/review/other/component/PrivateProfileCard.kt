package com.peto.ramap.ui.review.other.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_profile_private
import ramap.shared.generated.resources.review_profile_private

@Composable
internal fun PrivateProfileCard(
    text: String,
    modifier: Modifier = Modifier,
    image: DrawableResource = Res.drawable.ic_profile_private,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )

        AppText(
            text = text,
            style = AppTextStyle.H3Brand,
            color = GrayColor.C400,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivateProfileCardPreview() {
    RamapTheme {
        PrivateProfileCard(text = stringResource(Res.string.review_profile_private))
    }
}
