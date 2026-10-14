package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopMenuRepository

internal class DefaultShopMenuRepository(
    private val ramenShopRepository: RamenShopRepository,
) : ShopMenuRepository {
    override suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu> =
        when (val result = ramenShopRepository.fetchShopDetail(shopId)) {
            is RamapResult.Success -> RamapResult.Success(ShopMenu(result.data.menuSections, result.data.menuUpdatedAt))
            is RamapResult.Error -> result
        }
}
