package com.peto.ramap.domain.model.rank

import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.Category

data class RankingQuery(
    val area: AdministrativeArea?,
    val categories: Set<Category>,
    val cursor: RankingCursor? = null,
    val limit: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }
}
