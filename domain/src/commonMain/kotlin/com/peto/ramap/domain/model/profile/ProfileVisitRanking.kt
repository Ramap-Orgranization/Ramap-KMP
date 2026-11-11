package com.peto.ramap.domain.model.profile

data class ProfileVisitRanking(
    val values: List<ProfileVisitedShop>,
) {
    init {
        require(values.size <= MAX_SHOPS)
    }

    private val tiedVisitCounts =
        values
            .groupingBy(ProfileVisitedShop::visitCount)
            .eachCount()
            .filterValues { it > 1 }
            .keys

    val hasPodium = values.size >= PODIUM_SIZE && (0 until PODIUM_SIZE).all { rankAt(it) != null }

    val hasMoreShops: Boolean get() = hasPodium && values.size > PODIUM_SIZE

    fun rankAt(index: Int): Int? {
        val shop = values[index]
        return if (shop.isTied || shop.visitCount in tiedVisitCounts) null else index + 1
    }

    companion object {
        const val PODIUM_SIZE = 3
        private const val MAX_SHOPS = 10
    }
}
