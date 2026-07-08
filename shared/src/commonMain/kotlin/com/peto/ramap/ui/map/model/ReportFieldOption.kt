package com.peto.ramap.ui.map.model

import com.peto.ramap.domain.model.ShopInformationField
import org.jetbrains.compose.resources.StringResource

data class ReportFieldOption(
    val field: ShopInformationField,
    val label: StringResource,
)
