package com.peto.ramap.ui.resource.businesshours

import com.peto.ramap.ui.resource.UiText
import org.jetbrains.compose.resources.StringResource

data class BusinessHoursResourceLine(
    val dayLabel: StringResource,
    val endDayLabel: StringResource? = null,
    val values: List<UiText>,
)
