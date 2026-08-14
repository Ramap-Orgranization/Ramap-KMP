package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.shop.RamenShops

class RamenShopsPreviewParameterProvider : PreviewParameterProvider<RamenShops> {
    private val shopProvider = RamenShopPreviewParameterProvider()

    override val values: Sequence<RamenShops> =
        sequenceOf(RamenShops(shopProvider.ramenShopPreviewSamples))
}
