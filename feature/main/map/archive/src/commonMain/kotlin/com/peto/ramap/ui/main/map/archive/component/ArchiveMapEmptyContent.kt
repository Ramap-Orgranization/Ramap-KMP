package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.find_visited_shop
import ramap.shared.generated.resources.ic_notice_search
import ramap.shared.generated.resources.visited_shops_empty_description
import ramap.shared.generated.resources.visited_shops_empty_title

@Composable
internal fun ArchiveMapEmptyContent(
    onFindVisitedShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 32.dp)
                    .padding(bottom = if (maxHeight < 480.dp) 24.dp else 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArchiveMapIllustration()
            Spacer(modifier = Modifier.height(25.dp))
            AppText(
                text = stringResource(Res.string.visited_shops_empty_title),
                style = AppTextStyle.H3Brand,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppText(
                text = stringResource(Res.string.visited_shops_empty_description),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
                textAlign = TextAlign.Center,
            )
            AppButton(
                text = stringResource(Res.string.find_visited_shop),
                onClick = onFindVisitedShop,
                backgroundColor = ChromaticColor.Orange400,
                icon = Res.drawable.ic_notice_search,
                iconTint = CommonColor.White,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ArchiveMapEmptyContentPreview() {
    RamapTheme {
        ArchiveMapEmptyContent(onFindVisitedShop = {}, modifier = Modifier.fillMaxSize())
    }
}
