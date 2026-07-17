package com.peto.ramap.domain.model.shop

data class RamenShopFilter(
    private val values: Set<Category> = emptySet(),
) : Set<Category> by values {
    operator fun plus(category: Category): RamenShopFilter = copy(values = values + category)

    operator fun minus(category: Category): RamenShopFilter = copy(values = values - category)

    fun clear(): RamenShopFilter = copy(values = emptySet())
}
