package com.peto.ramap.designsystem.resource.businesshours

import com.peto.ramap.designsystem.resource.UiText
import org.jetbrains.compose.resources.StringResource

data class BusinessHoursResourceLine(
    val dayLabel: StringResource,
    val endDayLabel: StringResource? = null,
    val values: List<UiText>,
)
