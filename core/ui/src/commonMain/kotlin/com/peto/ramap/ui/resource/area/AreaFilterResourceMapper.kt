package com.peto.ramap.ui.resource.area

import com.peto.ramap.domain.model.shop.AreaFilter
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions

fun AreaFilter.label(): StringResource =
    when (this) {
        AreaFilter.Nationwide -> Res.string.ranking_all_regions
        is AreaFilter.Selected -> AdministrativeAreaResourceMapper.map(area).shortName
    }
