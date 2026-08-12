package com.peto.ramap.ui.resource.area

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.shop.AreaFilter
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions
import ramap.shared.generated.resources.ranking_district_label

object AreaFilterResourceMapper {
    fun label(areaFilter: AreaFilter): UiText =
        when (areaFilter) {
            AreaFilter.Nationwide -> UiText(Res.string.ranking_all_regions)
            is AreaFilter.Province -> UiText(AdministrativeAreaUiModel.map(areaFilter.area).shortName)
            is AreaFilter.District ->
                UiText(
                    resource = Res.string.ranking_district_label,
                    arguments = listOf(areaFilter.district.name),
                )
        }
}
