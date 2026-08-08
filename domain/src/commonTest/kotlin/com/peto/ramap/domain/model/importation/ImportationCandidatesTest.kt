package com.peto.ramap.domain.model.importation

import com.peto.ramap.domain.model.personalization.ShopPersonalization
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportationCandidatesTest {
    @Test
    fun `숨김과 이미 좋아요한 매장을 제외한 후보를 반환한다`() {
        val result =
            ImportationCandidates.from(
                preview =
                    ImportationPreview(
                        provider = ImportationProvider.KAKAO,
                        totalPlaceCount = 3,
                        matchedShopIds = setOf(IMPORTABLE_SHOP_ID, BOOKMARKED_SHOP_ID, HIDDEN_SHOP_ID),
                        unmatchedPlaceNames = emptyList(),
                    ),
                personalization =
                    ShopPersonalization(
                        bookmarkedShopIds = setOf(BOOKMARKED_SHOP_ID),
                        hiddenShopIds = setOf(HIDDEN_SHOP_ID),
                    ),
            )

        assertEquals(setOf(IMPORTABLE_SHOP_ID), result.importableShopIds)
        assertEquals(1, result.alreadyBookmarkedCount)
        assertEquals(1, result.hiddenCount)
    }

    private companion object {
        const val IMPORTABLE_SHOP_ID = "importable-shop"
        const val BOOKMARKED_SHOP_ID = "bookmarked-shop"
        const val HIDDEN_SHOP_ID = "hidden-shop"
    }
}
