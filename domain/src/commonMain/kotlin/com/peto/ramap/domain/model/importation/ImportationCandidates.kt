package com.peto.ramap.domain.model.importation

import com.peto.ramap.domain.model.personalization.Personalization

data class ImportationCandidates(
    val importableShopIds: Set<String>,
    val alreadyBookmarkedCount: Int,
) {
    companion object {
        fun from(
            preview: ImportationPreview,
            personalization: Personalization,
        ): ImportationCandidates {
            val alreadyBookmarkedShopIds = preview.matchedShopIds intersect personalization.bookmarkedShopIds
            return ImportationCandidates(
                importableShopIds = preview.matchedShopIds - alreadyBookmarkedShopIds,
                alreadyBookmarkedCount = alreadyBookmarkedShopIds.size,
            )
        }
    }
}
