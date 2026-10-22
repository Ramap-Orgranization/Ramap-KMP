package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository

/** 유효한 매장 검색어로 최대 50개를 조회한다. */
class SearchRamenShopsUseCase(
    private val ramenShopRepository: RamenShopRepository,
) {
    suspend operator fun invoke(
        query: SearchQuery,
        center: Location? = null,
    ): RamapResult<RamenShops> {
        val normalized = query.normalizeShopSearchQuery()
        if (!normalized.isValidForShopSearch()) return RamapResult.Success(RamenShops(emptyMap()))
        return ramenShopRepository.searchMapRamenShops(normalized, center, RESULT_LIMIT)
    }

    companion object {
        private const val RESULT_LIMIT = 50
    }
}
