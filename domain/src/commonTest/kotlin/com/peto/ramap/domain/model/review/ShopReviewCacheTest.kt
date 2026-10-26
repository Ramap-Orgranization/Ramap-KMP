package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.fake.FakeReviewChangeStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopReviewCacheTest {
    @Test
    fun `같은 매장도 열람 사용자와 비로그인 범위별로 페이지를 구분한다`() {
        val cache = ShopReviewCache(FakeReviewChangeStore(), ReviewChangeReducer())
        val keys = listOf(ShopReviewCacheKey("shop", "owner"), ShopReviewCacheKey("shop", "other"), ShopReviewCacheKey("shop", null), ShopReviewCacheKey("another-shop", "owner"))
        val pages = keys.mapIndexed { index, key -> ShopReviewsPage(listOf(review(key.shopId, "review-$index")), index + 1) }
        for ((index, key) in keys.withIndex()) cache.save(key, pages[index])
        for ((index, key) in keys.withIndex()) assertEquals(pages[index], cache.find(key))
    }

    @Test
    fun `리뷰 좋아요 변경은 해당 페이지에 반영하고 다른 매장 페이지는 보존한다`() {
        val changes = FakeReviewChangeStore()
        val cache = ShopReviewCache(changes, ReviewChangeReducer())
        val key = ShopReviewCacheKey("shop", "owner")
        val otherKey = ShopReviewCacheKey("another-shop", "owner")
        val row = review("shop", "review")
        val otherPage = ShopReviewsPage(listOf(review("another-shop", "other-review")), 1)
        cache.save(key, ShopReviewsPage(listOf(row), 1))
        cache.save(otherKey, otherPage)
        changes.publish(ReviewChange.Liked(row.id, ReviewLike(7, true)))
        assertEquals(ShopReviewsPage(listOf(row.copy(likeCount = 7, isLiked = true)), 1), cache.find(key))
        assertEquals(otherPage, cache.find(otherKey))
    }

    @Test
    fun `비공개 작성 변경은 작성자 범위에만 한 번 반영하고 비로그인 집계는 유지한다`() {
        val changes = FakeReviewChangeStore()
        val cache = ShopReviewCache(changes, ReviewChangeReducer())
        val owner = ShopReviewCacheKey("shop", "author")
        val anonymous = ShopReviewCacheKey("shop", null)
        val empty = ShopReviewsPage(emptyList(), 0)
        cache.save(owner, empty)
        cache.save(anonymous, empty)
        val written = review("shop", "private-review").copy(isPublic = false)
        changes.publish(ReviewChange.Written(written.id, "author", "shop", null, ReviewSnapshot(written, "매장"), ReviewOwnerCounts(1, 0, 1, 0, false)))
        val ownerPage = ShopReviewsPage(listOf(written), 1)
        assertEquals(ownerPage, cache.find(owner))
        assertEquals(ownerPage, cache.find(owner))
        assertEquals(empty, cache.find(anonymous))
    }

    @Test
    fun `세션 수명이 바뀌면 저장한 모든 매장과 사용자 범위를 무효화한다`() {
        val changes = FakeReviewChangeStore()
        val cache = ShopReviewCache(changes, ReviewChangeReducer())
        val keys = listOf(ShopReviewCacheKey("shop", "owner"), ShopReviewCacheKey("another-shop", null))
        for (key in keys) cache.save(key, ShopReviewsPage(listOf(review(key.shopId, "review")), 1))
        changes.reset("new-user")
        for (key in keys) assertNull(cache.find(key))
    }

    @Test
    fun `변경 기록이 유실되면 부분 갱신 대신 모든 캐시를 제거한다`() {
        val changes = FakeReviewChangeStore()
        val cache = ShopReviewCache(changes, ReviewChangeReducer())
        val keys = listOf(ShopReviewCacheKey("shop", "owner"), ShopReviewCacheKey("another-shop", null))
        for (key in keys) cache.save(key, ShopReviewsPage(listOf(review(key.shopId, "review")), 1))
        repeat(129) { changes.publish(ReviewChange.AccessChanged("unrelated")) }
        for (key in keys) assertNull(cache.find(key))
    }

    @Test
    fun `캐시를 비운 뒤 저장한 새 페이지에는 이전 변경을 다시 적용하지 않는다`() {
        val changes = FakeReviewChangeStore()
        val cache = ShopReviewCache(changes, ReviewChangeReducer())
        val key = ShopReviewCacheKey("shop", "owner")
        val row = review("shop", "review")
        cache.save(key, ShopReviewsPage(listOf(row), 1))
        changes.publish(ReviewChange.Liked(row.id, ReviewLike(7, true)))
        assertEquals(
            7,
            cache
                .find(key)
                ?.reviews
                ?.single()
                ?.likeCount,
        )
        cache.clear()
        assertNull(cache.find(key))
        val fresh = ShopReviewsPage(listOf(row.copy(likeCount = 12)), 1)
        cache.save(key, fresh)
        assertEquals(fresh, cache.find(key))
    }

    private fun review(
        shopId: String,
        id: String,
    ) = Review(id, shopId, "리뷰 본문", "2026-10-14", author = ReviewAuthor("author", "작성자"))
}
