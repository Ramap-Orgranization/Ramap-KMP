package com.peto.ramap.designsystem.component

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
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laduck_loading_walking

@Composable
fun ShopListEmptyContent(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.laduck_loading_walking),
            contentDescription = null,
            modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
        )
        AppText(
            text = title,
            style = AppTextStyle.H3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
