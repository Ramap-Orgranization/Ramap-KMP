package com.peto.ramap.domain.model.importation

import com.peto.ramap.domain.model.personalization.ShopPersonalization

data class ImportationCandidates(
    val importableShopIds: Set<String>,
    val alreadyBookmarkedCount: Int,
    val hiddenCount: Int,
) {
    companion object {
        fun from(
            preview: ImportationPreview,
            personalization: ShopPersonalization,
        ): ImportationCandidates {
            val hiddenShopIds = preview.matchedShopIds intersect personalization.hiddenShopIds
            val alreadyBookmarkedShopIds =
                (preview.matchedShopIds intersect personalization.bookmarkedShopIds) - hiddenShopIds
            val importableShopIds =
                preview.matchedShopIds - hiddenShopIds - alreadyBookmarkedShopIds

            return ImportationCandidates(
                importableShopIds = importableShopIds,
                alreadyBookmarkedCount = alreadyBookmarkedShopIds.size,
                hiddenCount = hiddenShopIds.size,
            )
        }
    }
}
