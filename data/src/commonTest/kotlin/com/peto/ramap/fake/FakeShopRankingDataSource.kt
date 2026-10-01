package com.peto.ramap.fake

import com.peto.ramap.data.datasource.ranking.AdministrativeDistrictParameters
import com.peto.ramap.data.datasource.ranking.ShopRankingDataSource
import com.peto.ramap.data.datasource.ranking.ShopRankingParameters
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.shop.AdministrativeDistricts

internal class FakeShopRankingDataSource(
    private val administrativeDistrictsByArea: Map<String, AdministrativeDistricts>,
    private val failuresBeforeSuccess: MutableMap<String, Int> = mutableMapOf(),
) : ShopRankingDataSource {
    val administrativeDistrictRequests = mutableListOf<AdministrativeDistrictParameters>()

    override suspend fun fetchShopRankings(parameter: ShopRankingParameters): RankingPage = error("Ranking fetch is not used in administrative district cache tests")

    override suspend fun fetchAdministrativeDistricts(parameter: AdministrativeDistrictParameters): AdministrativeDistricts {
        administrativeDistrictRequests += parameter

        val remainingFailures = failuresBeforeSuccess[parameter.area] ?: 0
        if (remainingFailures > 0) {
            failuresBeforeSuccess[parameter.area] = remainingFailures - 1
            error("Failed to fetch administrative districts")
        }
        return administrativeDistrictsByArea.getValue(parameter.area)
    }
}
