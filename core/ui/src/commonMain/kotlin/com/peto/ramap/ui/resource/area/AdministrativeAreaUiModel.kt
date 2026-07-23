package com.peto.ramap.ui.resource.area

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.shop.AdministrativeArea
import org.jetbrains.compose.resources.StringResource

@Immutable
data class AdministrativeAreaUiModel(
    val area: AdministrativeArea,
    val shortName: StringResource,
    val officialName: StringResource,
)
