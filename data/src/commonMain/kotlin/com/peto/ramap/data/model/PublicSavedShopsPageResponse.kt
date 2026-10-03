package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import kotlinx.serialization.Serializable

@Serializable
internal data class PublicSavedShopsPageResponse(
    val access: ProfileAccessResponse,
    val shops: List<RamenShopResponse> = emptyList(),
) {
    fun toDomain(): PublicSavedShopsPage =
        PublicSavedShopsPage(
            access = access.toDomain(),
            shops = shops.map { it.toDomain() },
        )
}
