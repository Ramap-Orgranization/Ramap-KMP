package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.community.CommunityDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ProfileReviewsPage
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultCommunityRepository(
    private val dataSource: CommunityDataSource,
    private val shopDataSource: RamenShopDataSource,
) : CommunityRepository {
    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchMyCommunityProfile().toDomain() }

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = invokeRequest { dataSource.fetchProfileAccess(userId).toDomain() }

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): RamapResult<ProfileReviewsPage> =
        invokeRequest {
            val page = dataSource.fetchProfileReviewsPage(userId, offset)
            val reviews = page.reviews.map { it.toDomain() }
            val shops = fetchReviewShops(reviews)
            ProfileReviewsPage(
                access = page.access.toDomain(),
                reviews = reviews.map { ProfileReview(review = it, shop = shops[it.shopId]) },
            )
        }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>> =
        invokeRequest {
            val reviews = dataSource.fetchUserReviews(userId, offset).map { it.toDomain() }
            val shopsById = fetchReviewShops(reviews)
            reviews.map { ProfileReview(review = it, shop = shopsById[it.shopId]) }
        }

    private suspend fun fetchReviewShops(reviews: List<Review>): Map<String, RamenShop> {
        if (reviews.isEmpty()) return emptyMap()
        val shopIds = reviews.map { it.shopId }.toSet()
        val result =
            invokeRequest {
                shopDataSource.fetchRamenShopsByIds(shopIds).map { it.toDomain() }.associateBy { it.id }
            }
        return when (result) {
            is RamapResult.Success -> result.data
            is RamapResult.Error -> emptyMap()
        }
    }

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage> = invokeRequest { dataSource.fetchUserSavedShops(userId, offset).toDomain() }

    override suspend fun fetchBlockedUsers(): RamapResult<List<BlockedUser>> = invokeRequest { dataSource.fetchBlockedUsers().map { it.toDomain() } }

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report(REPORT_TYPE_REVIEW, reviewId, reason, details)

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report(REPORT_TYPE_USER, userId, reason, details)

    private suspend fun report(
        type: String,
        id: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> =
        invokeRequest {
            require(details.length <= ReportReason.MAX_DETAILS_LENGTH) { ERROR_REPORT_DETAILS_TOO_LONG }
            dataSource.report(type, id, reason.name.lowercase(), details.trim())
        }

    override suspend fun blockUser(userId: String): RamapResult<Unit> = changeBlock(userId, blocked = true)

    override suspend fun unblockUser(userId: String): RamapResult<Unit> = changeBlock(userId, blocked = false)

    private suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            dataSource.changeBlock(userId, blocked)
        }

    private companion object {
        const val REPORT_TYPE_REVIEW = "review"
        const val REPORT_TYPE_USER = "user"
        const val ERROR_REPORT_DETAILS_TOO_LONG = "Report details too long"
    }
}
