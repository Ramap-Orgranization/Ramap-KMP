package com.peto.ramap.domain.model.shop

data class RamenShopFilter(
    private val values: Set<Category> = emptySet(),
    val hours: ShopHoursFilter = ShopHoursFilter.All,
) : Set<Category> by values {
    val hasCategoryFilter: Boolean
        get() = values.isNotEmpty()

    operator fun plus(category: Category): RamenShopFilter = copy(values = values + category)

    operator fun minus(category: Category): RamenShopFilter = copy(values = values - category)

    override fun isEmpty(): Boolean = values.isEmpty() && hours == ShopHoursFilter.All

    fun clear(): RamenShopFilter = RamenShopFilter()
}
