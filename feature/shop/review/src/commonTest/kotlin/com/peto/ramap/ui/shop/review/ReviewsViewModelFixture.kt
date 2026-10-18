package com.peto.ramap.ui.shop.review

import androidx.lifecycle.ViewModelStore
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.fake.FakeCommunityRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository

class ReviewsViewModelFixture {
    val login = FakeLoginRepository()
    val profile = FakeProfileRepository(null, isProfilePublic = false)
    val changes = FakeReviewChangeStore().apply { reset(null) }
    val reviews = FakeShopReviewRepository()
    val otherReview = Review("other", "shop", "맛있는 라멘", "2026-10-17", author = ReviewAuthor("author", "작성자"))
    val ownReview = otherReview.copy(id = "mine", author = ReviewAuthor("owner", "나"), isPublic = false)
    val likeRequests = mutableListOf<Pair<String, Boolean>>()
    var pageRequests = 0
        private set
    var profileRequests = 0
        private set
    var fetchPage: suspend (String, Long) -> RamapResult<ShopReviewsPage> = reviews::fetchShopReviewsPage
    var fetchProfile: suspend () -> RamapResult<AccountProfile> = profile::fetchMyProfile
    var updateLike: suspend (Boolean) -> RamapResult<ReviewLike> = { liked -> RamapResult.Success(ReviewLike(if (liked) 1 else 0, liked)) }

    private val repository =
        object : ReviewRepository by reviews {
            override suspend fun fetchShopReviewsPage(
                shopId: String,
                offset: Long,
            ): RamapResult<ShopReviewsPage> {
                pageRequests++
                return fetchPage(shopId, offset)
            }

            override suspend fun setReviewLike(
                reviewId: String,
                liked: Boolean,
            ): RamapResult<ReviewLike> {
                likeRequests += reviewId to liked
                return updateLike(liked)
            }
        }
    private val profiles =
        object : ProfileRepository by profile {
            override suspend fun fetchMyProfile(): RamapResult<AccountProfile> {
                profileRequests++
                return fetchProfile()
            }
        }
    private val reducer = ReviewChangeReducer()
    val viewModel = ReviewsViewModel(repository, FakeCommunityRepository(), profiles, login, changes, reducer, FeatchShopReviewsUseCase(repository, changes, reducer, ShopReviewCache(changes, reducer)))
    val store = ViewModelStore().apply { put("reviews", viewModel) }
}
