package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdministrativeDistrictResponse(
    @SerialName("sigungu") val name: String,
) {
    fun toDomain(): AdministrativeDistrict = AdministrativeDistrict(name)
}
