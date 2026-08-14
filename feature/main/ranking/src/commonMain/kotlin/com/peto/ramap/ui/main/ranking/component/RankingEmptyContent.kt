package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_empty_action
import ramap.shared.generated.resources.ranking_empty_description
import ramap.shared.generated.resources.ranking_empty_title

@Composable
internal fun RankingEmptyContent(
    onFindShopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = stringResource(Res.string.ranking_empty_title),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
        )
        Spacer(Modifier.height(8.dp))
        AppText(
            text = stringResource(Res.string.ranking_empty_description),
            style = AppTextStyle.B2,
            color = GrayColor.C300,
        )
        Spacer(Modifier.height(20.dp))
        AppButton(
            text = stringResource(Res.string.ranking_empty_action),
            modifier = Modifier.fillMaxWidth(),
            onClick = onFindShopClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingEmptyContentPreview() {
    RamapTheme {
        RankingEmptyContent(onFindShopClick = {})
    }
}
