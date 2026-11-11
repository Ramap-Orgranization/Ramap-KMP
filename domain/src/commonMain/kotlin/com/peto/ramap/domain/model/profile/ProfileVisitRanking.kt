package com.peto.ramap.domain.model.profile

data class ProfileVisitRanking(
    val values: List<ProfileVisitedShop>,
) {
    init {
        require(values.size <= MAX_SHOPS)
    }

    private companion object {
        const val MAX_SHOPS = 10
    }
}
