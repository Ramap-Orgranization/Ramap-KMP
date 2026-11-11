package com.peto.ramap.data.model

import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.model.profile.ProfileVisitedShop
import com.peto.ramap.network.client.media.MediaBucket
import com.peto.ramap.network.client.media.MediaPath
import com.peto.ramap.network.client.media.PublicMediaUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileVisitRankingResponse(
    val available: Boolean,
    val shops: List<Shop>,
) {
    fun toDomain(): ProfileVisitRanking? =
        if (available) {
            ProfileVisitRanking(shops.map(Shop::toDomain))
        } else {
            null
        }

    @Serializable
    data class Shop(
        @SerialName("shop_id") val id: String,
        @SerialName("shop_name") val name: String,
        @SerialName("visit_count") val visitCount: Long,
        @SerialName("instagram_profile_image_path") val instagramProfileImagePath: String? = null,
        @SerialName("is_tied") val isTied: Boolean = false,
    ) {
        fun toDomain(): ProfileVisitedShop =
            ProfileVisitedShop(
                id = id,
                name = name,
                visitCount = visitCount,
                instagramProfileImageUrl =
                    MediaPath.from(instagramProfileImagePath)?.let { path ->
                        PublicMediaUrl(MediaBucket.SHOP_PROFILE_IMAGES, path).resolve()
                    },
                isTied = isTied,
            )
    }
}
