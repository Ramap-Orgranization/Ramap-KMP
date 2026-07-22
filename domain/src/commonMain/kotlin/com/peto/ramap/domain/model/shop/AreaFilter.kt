package com.peto.ramap.domain.model.shop

sealed interface AreaFilter {
    data object Nationwide : AreaFilter

    data class Selected(
        val area: AdministrativeArea,
    ) : AreaFilter
}
