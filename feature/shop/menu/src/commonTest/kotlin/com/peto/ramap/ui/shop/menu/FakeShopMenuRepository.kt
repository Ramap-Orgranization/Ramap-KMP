package com.peto.ramap.ui.shop.menu

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.repository.ShopMenuRepository

class FakeShopMenuRepository : ShopMenuRepository {
    val calls = mutableListOf<String>()
    var handler: suspend (String) -> RamapResult<ShopMenu> = { RamapResult.Success(ShopMenu(emptyList(), it)) }

    override suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu> {
        calls += shopId
        return handler(shopId)
    }
}
