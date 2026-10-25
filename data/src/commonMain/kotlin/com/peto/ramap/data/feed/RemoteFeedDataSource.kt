package com.peto.ramap.data.feed

import com.peto.ramap.data.datasource.review.ReviewImageResolver
import com.peto.ramap.data.model.CommunityFeedPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.community.CommunityFeedCursor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteFeedDataSource(
    private val client: SupabaseClient,
    private val images: ReviewImageResolver,
) : FeedDataSource {
    override suspend fun fetchCommunityReviewsPage(cursor: CommunityFeedCursor?): CommunityFeedPageResponse {
        val page =
            client.postgrest
                .rpc(
                    RPC_FETCH_COMMUNITY_REVIEWS_PAGE,
                    buildJsonObject {
                        put(PARAM_CREATED_AT, cursor?.createdAt)
                        put(PARAM_REVIEW_ID, cursor?.reviewId)
                    },
                ).decodeAs<CommunityFeedPageResponse>()
        return page.copy(reviews = images.resolveProfileReviews(page.reviews))
    }

    override suspend fun fetchCommunityBlockedReviewOnce(reviewId: String): ReviewResponse? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_COMMUNITY_BLOCKED_REVIEW_ONCE,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeAs<ReviewResponse?>() ?: return null
        return images.resolveReviews(listOf(review)).single()
    }

    private companion object {
        const val RPC_FETCH_COMMUNITY_REVIEWS_PAGE = "fetch_community_reviews_page_v2"
        const val RPC_FETCH_COMMUNITY_BLOCKED_REVIEW_ONCE = "fetch_community_blocked_review_once"
        const val PARAM_CREATED_AT = "p_created_at"
        const val PARAM_REVIEW_ID = "p_review_id"
    }
}
