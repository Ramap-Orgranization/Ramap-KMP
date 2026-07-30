package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.shop.AreaFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdministrativeDistrictParameters(
    @SerialName("p_area") val area: String,
) {
    private fun areaName(areaFilter: AreaFilter): String? =
        when (areaFilter) {
            AreaFilter.Nationwide -> null
            is AreaFilter.Province -> areaFilter.area.name
            is AreaFilter.District -> areaFilter.area.name
        }
}
