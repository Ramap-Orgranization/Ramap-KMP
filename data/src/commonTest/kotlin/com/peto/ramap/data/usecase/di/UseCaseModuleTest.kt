package com.peto.ramap.data.usecase.di

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class UseCaseModuleTest {
    @Test
    fun `리뷰 조회 유즈케이스를 새로 주입받으면 이전 화면의 캐시를 공유하지 않는다`() {
        val application =
            koinApplication {
                modules(
                    useCaseModule,
                    module {
                        single<ReviewRepository> { FakeShopReviewRepository() }
                        single<ReviewChangeStore> { FakeReviewChangeStore() }
                    },
                )
            }
        try {
            val first = application.koin.get<FeatchShopReviewsUseCase>()
            val page = ShopReviewsPage(listOf(Review("review", "shop", "리뷰 본문", "2026-10-14")), 1)
            first.savePage("shop", "owner", page)
            val second = application.koin.get<FeatchShopReviewsUseCase>()
            assertNotSame(first, second)
            assertEquals(page, first.findCached("shop", "owner"))
            assertNull(second.findCached("shop", "owner"))
        } finally {
            application.close()
        }
    }
}
