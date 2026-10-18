package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter

class AreaFilterPreviewParameterProvider : PreviewParameterProvider<AreaFilter> {
    override val values: Sequence<AreaFilter> =
        sequenceOf(
            AreaFilter.Nationwide,
            AreaFilter.Province(AdministrativeArea.SEOUL),
            AreaFilter.District(AdministrativeArea.SEOUL, AdministrativeDistrict("마포구")),
        )
}
