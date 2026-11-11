package com.peto.ramap.data.model

import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileVisitRankingResponse(
    val available: Boolean,
    val shops: List<Shop>,
) {
    fun toDomain(): ProfileVisitRanking? =
        if (available) {
            ProfileVisitRanking(shops.map { ProfileVisitedShop(it.id, it.name, it.visitCount) })
        } else {
            null
        }

    @Serializable
    data class Shop(
        @SerialName("shop_id") val id: String,
        @SerialName("shop_name") val name: String,
        @SerialName("visit_count") val visitCount: Long,
    )
}
