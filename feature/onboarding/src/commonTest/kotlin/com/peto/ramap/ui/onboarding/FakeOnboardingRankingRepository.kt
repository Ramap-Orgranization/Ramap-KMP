package com.peto.ramap.ui.onboarding

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ShopRankingRepository

internal class FakeOnboardingRankingRepository(
    private val shops: RamenShops,
) : ShopRankingRepository {
    var error: RamapError? = null

    override suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage> =
        error?.let { RamapResult.Error(it) }
            ?: RamapResult.Success(RankingPage(ShopRankings(shops.values.map { ShopRanking(it, 10L) }), null))

    override suspend fun fetchAdministrativeDistricts(area: AdministrativeArea): RamapResult<AdministrativeDistricts> = RamapResult.Success(AdministrativeDistricts(emptyList()))
}
