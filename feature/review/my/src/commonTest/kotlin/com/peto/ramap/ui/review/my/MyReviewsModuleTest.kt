package com.peto.ramap.ui.review.my

import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.ui.review.my.di.reviewMyModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MyReviewsModuleTest {
    @Test
    fun `내 리뷰 모듈에서 뷰모델을 주입받을 수 있다`() =
        coroutinesTest {
            val application =
                koinApplication {
                    modules(
                        reviewMyModule,
                        module {
                            singleOf(::ReviewChangeReducer)
                            single<com.peto.ramap.domain.store.ReviewChangeStore> {
                                com.peto.ramap.fake
                                    .FakeReviewChangeStore()
                            }
                            single<ReviewRepository> { FakeOwnerReviews() }
                            single<ProfileRepository> { FakeProfileRepository() }
                        },
                    )
                }

            try {
                val viewModel = application.koin.get<MyReviewsViewModel>()
                assertNotNull(viewModel)
                runCurrent()
            } finally {
                application.close()
            }
        }
}
