package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ShopRankingRepository
import kotlinx.coroutines.CompletableDeferred

public class FakeOnboardingRankingRepository(
    private val shops: RamenShops,
) : ShopRankingRepository {
    public var error: RamapError? = null
    public var pages: Map<RankingCursor?, RankingPage> = mapOf(null to RankingPage(ShopRankings(shops.values.map { ShopRanking(it, 10L) }), null))
    public var pendingRequest: CompletableDeferred<Unit>? = null
    public val requestedQueries: MutableList<RankingQuery> = mutableListOf()

    override suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage> {
        requestedQueries += query
        pendingRequest?.await()
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(pages.getValue(query.cursor))
    }

    override suspend fun fetchAdministrativeDistricts(area: AdministrativeArea): RamapResult<AdministrativeDistricts> = RamapResult.Success(AdministrativeDistricts(emptyList()))
}
