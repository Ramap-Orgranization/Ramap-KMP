package com.peto.ramap.domain.model.shop

data class MenuCategories(
    private val values: List<Category>,
) : List<Category> by values {
    val hasCategory: Boolean
        get() = values.isNotEmpty()

    fun matches(filter: RamenShopFilter): Boolean =
        filter.isEmpty() ||
            values.any { category ->
                category in filter
            }
}
