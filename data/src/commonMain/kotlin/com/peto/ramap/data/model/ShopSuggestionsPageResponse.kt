package com.peto.ramap.data.model

import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage
import com.peto.ramap.domain.model.shop.RamenShops
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopSuggestionsPageResponse(
    val shops: List<RamenShopResponse>,
    @SerialName("next_cursor") val nextCursor: String?,
) {
    fun toDomain(): ShopSuggestionsPage =
        ShopSuggestionsPage(
            shops = RamenShops(shops.map(RamenShopResponse::toDomain)),
            nextCursor = nextCursor?.let(::ShopSuggestionsCursor),
        )
}
