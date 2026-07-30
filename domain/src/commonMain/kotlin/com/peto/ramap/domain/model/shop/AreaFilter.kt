package com.peto.ramap.domain.model.shop

sealed interface AreaFilter {
    data object Nationwide : AreaFilter

    data class Province(
        val area: AdministrativeArea,
    ) : AreaFilter

    data class District(
        val area: AdministrativeArea,
        val district: AdministrativeDistrict,
    ) : AreaFilter
}
