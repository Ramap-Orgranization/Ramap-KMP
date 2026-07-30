package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.ranking.AdministrativeDistrictParameters
import com.peto.ramap.data.datasource.ranking.ShopRankingDataSource
import com.peto.ramap.data.datasource.ranking.ShopRankingParameters
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopRankingRepository(
    private val dataSource: ShopRankingDataSource,
) : ShopRankingRepository {
    private val administrativeDistrictsCache = mutableMapOf<AdministrativeArea, AdministrativeDistricts>()

    override suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage> =
        invokeRequest {
            val params =
                ShopRankingParameters(
                    area =
                        when (val filter = query.areaFilter) {
                            AreaFilter.Nationwide -> null
                            is AreaFilter.Province -> filter.area.name
                            is AreaFilter.District -> filter.area.name
                        },
                    district =
                        when (val filter = query.areaFilter) {
                            AreaFilter.Nationwide,
                            is AreaFilter.Province,
                            -> null

                            is AreaFilter.District -> filter.district.name
                        },
                    categoryIds = query.categories.map { category -> category.id },
                    cursorLikeCount = query.cursor?.likeCount,
                    cursorName = query.cursor?.name,
                    cursorId = query.cursor?.shopId,
                    limit = query.limit,
                )

            dataSource.fetchShopRankings(params)
        }

    override suspend fun fetchAdministrativeDistricts(area: AdministrativeArea): RamapResult<AdministrativeDistricts> {
        administrativeDistrictsCache[area]?.let { return RamapResult.Success(it) }

        val result =
            invokeRequest {
                dataSource.fetchAdministrativeDistricts(
                    AdministrativeDistrictParameters(
                        area.name,
                    ),
                )
            }
        if (result is RamapResult.Success) {
            administrativeDistrictsCache[area] = result.data
        }
        return result
    }
}
