package com.peto.ramap.data.datasource.shop

internal data class ShopTextSearchFilter(
    val pattern: String,
    val isVisible: Boolean,
    val columns: List<String>,
) {
    companion object {
        fun forVisibleShops(pattern: String): ShopTextSearchFilter =
            ShopTextSearchFilter(
                pattern = pattern,
                isVisible = true,
                columns = SEARCH_COLUMNS,
            )

        private val SEARCH_COLUMNS =
            listOf(
                "name",
                "address",
                "phone",
                "business_hours",
                "business_hours_notice",
            )
    }
}
