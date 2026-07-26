package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.shop.AdministrativeDistricts

internal interface ShopRankingDataSource {
    suspend fun fetchShopRankings(parameter: ShopRankingParameters): RankingPage

    suspend fun fetchAdministrativeDistricts(parameter: AdministrativeDistrictParameters): AdministrativeDistricts
}
