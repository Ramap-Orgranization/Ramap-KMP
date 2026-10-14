package com.peto.ramap.ui.main

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.repository.ShopMenuRepository

class FakeMapShopMenuRepository(
    var result: RamapResult<ShopMenu> = RamapResult.Success(ShopMenu(emptyList())),
) : ShopMenuRepository {
    val requestedShopIds = mutableListOf<String>()

    override suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu> {
        requestedShopIds += shopId
        return result
    }
}
