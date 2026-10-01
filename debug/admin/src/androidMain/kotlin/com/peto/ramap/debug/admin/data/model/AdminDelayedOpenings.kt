package com.peto.ramap.debug.admin.data.model

internal data class AdminDelayedOpenings(
    val koreaToday: String,
    val notices: List<AdminDelayedOpening>,
)
