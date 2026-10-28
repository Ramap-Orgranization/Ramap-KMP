package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.rememberBalloonBuilder
import com.skydoves.balloon.rememberBalloonState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
internal fun ArchiveMapClusterPopup(
    shops: List<RamenShop>,
    offset: IntOffset?,
    onShopClick: (RamenShop) -> Unit,
    onDismiss: () -> Unit,
) {
    val builder =
        rememberBalloonBuilder {
            setArrowSize(10.dp)
            setArrowPosition(0.5f)
            setCornerRadius(8.dp)
            setBackgroundColor(CommonColor.White)
            setBorder(GrayColor.C200, 1.dp)
            setPadding(15.dp)
            setMinWidth(210.dp)
            setMaxWidth(310.dp)
            setDismissWhenTouchOutside(true)
            setDismissWhenBackPressed(true)
        }
    val balloon = rememberBalloonState(builder)
    balloon.onDismiss = onDismiss
    LaunchedEffect(shops, offset) { balloon.showAlignTop() }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (offset == null) Alignment.Center else Alignment.TopStart,
    ) {
        Balloon(
            state = balloon,
            modifier = Modifier.size(1.dp).then(offset?.let { Modifier.offset { it } } ?: Modifier),
            balloonContent = {
                ArchiveClusterShopList(
                    shops = shops,
                    operatingNotices = emptyList(),
                    currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul")),
                    onShopClick = onShopClick,
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                )
            },
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveMapClusterPopupPreview() {
    RamapTheme {
        ArchiveMapClusterPopup(
            shops = RamenShopPreviewParameterProvider().ramenShopPreviewSamples,
            offset = null,
            onShopClick = {},
            onDismiss = {},
        )
    }
}
