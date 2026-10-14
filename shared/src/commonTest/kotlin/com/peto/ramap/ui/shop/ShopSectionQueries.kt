package com.peto.ramap.ui.shop

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.ShopMenuRepository
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ShopSectionQueries :
    FetchShopDetailUseCase,
    ShopMenuRepository,
    ReviewRepository by FakeShopReviewRepository() {
    val overviewCalls = mutableListOf<String>()
    val menuCalls = mutableListOf<String>()
    val reviewCalls = mutableListOf<Pair<String, Long>>()
    var overviewGate: CompletableDeferred<RamapResult<ShopDetail>>? = null
    var overviewResult: RamapResult<ShopDetail>? = null
    var menuGate: CompletableDeferred<ShopMenu>? = null
    var menuResult: RamapResult<ShopMenu> = RamapResult.Success(ShopMenu(emptyList(), "2026-10-14"))
    var reviewResult: RamapResult<ShopReviewsPage> = RamapResult.Success(ShopReviewsPage(listOf(Review("review", "shop", "리뷰 본문", "2026-10-14")), 1))

    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        overviewCalls += shopId
        return overviewGate?.await() ?: overviewResult ?: RamapResult.Success(ShopDetail(ramenShopFixture(id = shopId), 0L, null, null, null))
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup = ShopDetailCacheLookup.Miss

    override fun clearCache() = Unit

    override fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    ) = Unit

    override suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu> {
        menuCalls += shopId
        val pending = menuGate
        return if (pending == null) menuResult else RamapResult.Success(withContext(NonCancellable) { pending.await() })
    }

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> {
        reviewCalls += shopId to offset
        return reviewResult
    }
}
