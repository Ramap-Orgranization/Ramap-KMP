package com.peto.ramap.domain.model.shop

data class ShopRankings(
    private val rankings: List<ShopRanking>,
) : List<ShopRanking> by rankings {
    /**
     * 조건에 맞는 매장만 남겨 좋아요 수가 높은 순서의 랭킹을 반환한다.
     *
     * 숨긴 매장은 항상 제외하며, [areaFilter]가 [AreaFilter.Selected]이면 주소가 선택 지역과
     * 일치하는 매장만 남긴다. 카테고리는 [selectedCategories] 중 하나 이상을 포함할 때 선택되며,
     * 비어 있으면 모든 카테고리를 포함한다. 좋아요 수가 같으면 이름과 ID 순으로 정렬하고 같은
     * 순위를 부여하며, 다음 좋아요 수에는 바로 다음 순위를 부여한다.
     *
     * @param areaFilter 전국 또는 선택한 행정구역을 나타내는 지역 필터.
     * @param selectedCategories 선택한 메뉴 카테고리. 비어 있으면 모든 카테고리를 포함한다.
     * @param hiddenShopIds 결과에서 제외할 매장 ID.
     * @return 필터와 dense ranking이 적용된 매장 목록.
     */
    fun filterAndRank(
        areaFilter: AreaFilter,
        selectedCategories: Set<Category>,
        hiddenShopIds: Set<String>,
    ): List<RankedShop> {
        val filtered =
            rankings.filter { ranking ->
                matches(ranking, areaFilter, selectedCategories, hiddenShopIds)
            }
        val sorted = filtered.sortedWith(RANKING_COMPARATOR)
        return toDenseRankedShops(sorted)
    }

    private fun matches(
        ranking: ShopRanking,
        areaFilter: AreaFilter,
        selectedCategories: Set<Category>,
        hiddenShopIds: Set<String>,
    ): Boolean {
        val matchesArea =
            when (areaFilter) {
                AreaFilter.Nationwide -> true
                is AreaFilter.Selected ->
                    AdministrativeAreaParser.matches(
                        area = areaFilter.area,
                        address = ranking.shop.address,
                    )
            }
        return ranking.shop.id !in hiddenShopIds &&
            matchesArea &&
            (selectedCategories.isEmpty() || ranking.shop.menuCategories.any(selectedCategories::contains))
    }

    private fun toDenseRankedShops(rankings: List<ShopRanking>): List<RankedShop> {
        var rank = 0
        var previousLikeCount: Long? = null
        return rankings.map { shopRanking ->
            if (previousLikeCount != shopRanking.likeCount) {
                rank += 1
                previousLikeCount = shopRanking.likeCount
            }
            RankedShop(rank = rank, ranking = shopRanking)
        }
    }

    companion object {
        private val RANKING_COMPARATOR =
            compareByDescending<ShopRanking> { ranking -> ranking.likeCount }
                .thenBy { ranking -> ranking.shop.name }
                .thenBy { ranking -> ranking.shop.id }
    }
}
