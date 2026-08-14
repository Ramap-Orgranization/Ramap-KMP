package com.peto.ramap.domain.model.shop

data class RamenShopFilter(
    private val values: Set<Category> = emptySet(),
    val isOpenSelected: Boolean = false,
) : Set<Category> by values {
    val hasCategoryFilter: Boolean
        get() = values.isNotEmpty()

    operator fun plus(category: Category): RamenShopFilter = copy(values = values + category)

    operator fun minus(category: Category): RamenShopFilter = copy(values = values - category)

    override fun isEmpty(): Boolean = values.isEmpty() && !isOpenSelected

    fun clear(): RamenShopFilter =
        copy(
            values = emptySet(),
            isOpenSelected = false,
        )
}
