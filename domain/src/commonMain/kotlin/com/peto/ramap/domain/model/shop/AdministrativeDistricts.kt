package com.peto.ramap.domain.model.shop

data class AdministrativeDistricts(
    private val values: List<AdministrativeDistrict>,
) : List<AdministrativeDistrict> by values
