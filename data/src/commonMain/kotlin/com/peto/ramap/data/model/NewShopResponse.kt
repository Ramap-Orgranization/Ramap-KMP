package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpening
import com.peto.ramap.network.client.media.PublicMediaUrl
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewShopResponse(
    val id: String,
    val name: String,
    @SerialName("opening_date") val openingDate: LocalDate,
    @SerialName("opening_kind") val openingKind: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
) {
    fun toDomain(): NewShop =
        NewShop(
            id = id,
            name = name,
            opening = ShopOpening(openingDate, openingKind()),
            coverImageUrl = resolveCoverImage(),
        )

    private fun openingKind(): ShopOpening.Kind =
        when (openingKind) {
            "official" -> ShopOpening.Kind.OFFICIAL
            "soft" -> ShopOpening.Kind.SOFT
            else -> ShopOpening.Kind.OPENING
        }

    private fun resolveCoverImage(): String? {
        val value = PublicMediaUrl.resolveOwnedUrl(coverImageUrl) ?: return null
        if (!value.startsWith("https://", ignoreCase = true)) return null
        val url = runCatching { Url(value) }.getOrNull() ?: return null
        return value.takeIf {
            url.protocol == URLProtocol.HTTPS &&
                url.host.isNotBlank() &&
                url.user.isNullOrEmpty() &&
                url.password.isNullOrEmpty()
        }
    }
}
