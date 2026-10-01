package com.peto.ramap.ui.review.my

import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.ui.review.my.di.reviewMyModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MyReviewsModuleTest {
    @Test
    fun `my reviews view model resolves from module`() =
        coroutinesTest {
            val application =
                koinApplication {
                    modules(
                        reviewMyModule,
                        module {
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
