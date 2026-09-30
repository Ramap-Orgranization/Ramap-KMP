package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_empty_illustration
import ramap.shared.generated.resources.shop_review_empty

@Composable
fun ReviewEmptyContent(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.review_empty_illustration),
            contentDescription = null,
            modifier = Modifier.size(220.dp),
        )
        AppText(
            text = text,
            style = AppTextStyle.H3Brand,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewEmptyContentPreview() {
    RamapTheme {
        ReviewEmptyContent(text = stringResource(Res.string.shop_review_empty))
    }
}
