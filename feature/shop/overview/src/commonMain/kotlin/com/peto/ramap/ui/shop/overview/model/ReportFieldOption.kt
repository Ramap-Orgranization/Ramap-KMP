package com.peto.ramap.ui.shop.overview.model

import com.peto.ramap.domain.model.report.ShopInformationField
import org.jetbrains.compose.resources.StringResource

data class ReportFieldOption(
    val field: ShopInformationField,
    val label: StringResource,
)
