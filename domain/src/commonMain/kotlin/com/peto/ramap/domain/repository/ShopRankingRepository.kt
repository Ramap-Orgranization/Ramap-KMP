package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistricts

interface ShopRankingRepository {
    suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage>

    suspend fun fetchAdministrativeDistricts(area: AdministrativeArea): RamapResult<AdministrativeDistricts>
}
