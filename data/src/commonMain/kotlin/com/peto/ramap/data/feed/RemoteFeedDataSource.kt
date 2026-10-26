package com.peto.ramap.data.feed

import com.peto.ramap.data.datasource.review.ReviewImageResolver
import com.peto.ramap.data.model.ReviewFeedPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteFeedDataSource(
    private val client: SupabaseClient,
    private val images: ReviewImageResolver,
) : FeedDataSource {
    override suspend fun fetchReviewsPage(
        cursor: ReviewFeedCursor?,
        sort: ReviewFeedSort,
    ): ReviewFeedPageResponse {
        val rpc =
            when (sort) {
                ReviewFeedSort.LATEST -> RPC_FETCH_REVIEW_FEED_PAGE
                ReviewFeedSort.POPULAR -> RPC_FETCH_POPULAR_REVIEW_FEED_PAGE
            }
        val page =
            client.postgrest
                .rpc(
                    rpc,
                    buildJsonObject {
                        put(PARAM_CREATED_AT, cursor?.createdAt)
                        put(PARAM_REVIEW_ID, cursor?.reviewId)
                        if (sort == ReviewFeedSort.POPULAR) put(PARAM_LIKE_COUNT, cursor?.likeCount)
                    },
                ).decodeAs<ReviewFeedPageResponse>()
        return page.copy(reviews = images.resolveProfileReviews(page.reviews))
    }

    override suspend fun fetchBlockedReviewOnce(reviewId: String): ReviewResponse? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_BLOCKED_REVIEW_FEED_ONCE,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeAs<ReviewResponse?>() ?: return null
        return images.resolveReviews(listOf(review)).single()
    }

    private companion object {
        const val RPC_FETCH_POPULAR_REVIEW_FEED_PAGE = "fetch_popular_review_feed_page"
        const val RPC_FETCH_REVIEW_FEED_PAGE = "fetch_review_feed_page"
        const val RPC_FETCH_BLOCKED_REVIEW_FEED_ONCE = "fetch_blocked_review_feed_once"
        const val PARAM_LIKE_COUNT = "p_like_count"
        const val PARAM_CREATED_AT = "p_created_at"
        const val PARAM_REVIEW_ID = "p_review_id"
    }
}
