package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FeatchShopReviewsUseCaseTest {
    @Test
    fun `조회 중 접근 권한이 바뀌면 서버 집계를 재조회한다`() =
        runTest {
            val fake = FakeShopReviewRepository()
            val changes = FakeReviewChangeStore()
            var calls = 0
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        calls++
                        if (calls == 1) changes.publish(ReviewChange.AccessChanged("author"))
                        return RamapResult.Success(ShopReviewsPage(emptyList(), if (calls == 1) 5 else 0))
                    }
                }
            val page = useCase(repository, changes)("shop", null, ShopReviewsPage(emptyList(), 0), false).getOrThrow()
            assertEquals(2, calls)
            assertEquals(0, page.totalCount)
        }

    @Test
    fun `조회 도중 로그인 범위가 바뀌면 이전 사용자 응답을 취소한다`() =
        runTest {
            val fake = FakeShopReviewRepository()
            val changes = FakeReviewChangeStore()
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        changes.reset("new-user")
                        return RamapResult.Success(ShopReviewsPage(emptyList(), 0))
                    }
                }
            val useCase = useCase(repository, changes)
            assertFailsWith<CancellationException> { useCase("shop", "old-user", ShopReviewsPage(emptyList(), 0), false) }
        }

    @Test
    fun `추가 페이지는 중복 리뷰를 합치고 서버 전체 개수를 유지한다`() =
        runTest {
            val fake = FakeShopReviewRepository()
            val first = Review("first", "shop", "첫 리뷰", "2026-10-14")
            val second = Review("second", "shop", "두번째 리뷰", "2026-10-13")
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        assertEquals(1L, offset)
                        return RamapResult.Success(ShopReviewsPage(listOf(first, second), 2))
                    }
                }
            val page = useCase(repository, FakeReviewChangeStore())("shop", null, ShopReviewsPage(listOf(first), 2), true).getOrThrow()
            assertEquals(listOf(first, second), page.reviews)
            assertEquals(2, page.totalCount)
        }

    @Test
    fun `추가 조회 도중 기존 리뷰가 삭제되면 첫 페이지부터 복구해 삭제된 행과 누락을 남기지 않는다`() =
        runTest {
            val fake = FakeShopReviewRepository()
            val changes = FakeReviewChangeStore()
            val deleted = Review("deleted", "shop", "삭제할 리뷰", "2026-10-14")
            val rows = (1..4).map { Review("review-$it", "shop", "리뷰 본문", "2026-10-13") }
            val offsets = mutableListOf<Long>()
            val repository =
                object : ReviewRepository by fake {
                    override suspend fun fetchShopReviewsPage(
                        shopId: String,
                        offset: Long,
                    ): RamapResult<ShopReviewsPage> {
                        offsets += offset
                        if (offsets.size == 1) {
                            changes.publish(ReviewChange.Written(deleted.id, "author", "shop", ReviewSnapshot(deleted, "매장"), null, ReviewOwnerCounts(4, 4, 0, 4, true)))
                            return RamapResult.Success(ShopReviewsPage(rows.drop(1).take(2), 4))
                        }
                        return RamapResult.Success(ShopReviewsPage(rows.drop(offset.toInt()).take(2), 4))
                    }
                }
            val page = useCase(repository, changes)("shop", null, ShopReviewsPage(listOf(deleted), 5), true).getOrThrow()
            assertEquals(listOf(1L, 0L, 2L), offsets)
            assertEquals(rows, page.reviews)
            assertEquals(4, page.totalCount)
        }

    @Test
    fun `리뷰 캐시는 로그인 범위 변경이나 변경 기록 유실 시 제거한다`() {
        val changes = FakeReviewChangeStore()
        val useCase = useCase(FakeShopReviewRepository(), changes)
        val page = ShopReviewsPage(listOf(Review("review", "shop", "리뷰 본문", "2026-10-14")), 1)
        useCase.savePage("shop", "owner", page)
        assertEquals(page, useCase.findCached("shop", "owner"))
        assertEquals(null, useCase.findCached("shop", "other"))
        changes.reset("other")
        assertEquals(null, useCase.findCached("shop", "owner"))
        useCase.savePage("shop", "other", page)
        repeat(129) { changes.publish(ReviewChange.AccessChanged("unrelated")) }
        assertEquals(null, useCase.findCached("shop", "other"))
    }

    private fun useCase(
        repository: ReviewRepository,
        changes: FakeReviewChangeStore,
    ): FeatchShopReviewsUseCase {
        val reducer = ReviewChangeReducer()
        return FeatchShopReviewsUseCase(repository, changes, reducer, ShopReviewCache(changes, reducer))
    }
}
