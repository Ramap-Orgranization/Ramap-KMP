package com.peto.ramap.data.datasource.review

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.ReviewCommentCountResponse
import com.peto.ramap.data.model.ReviewCommentDeletionResponse
import com.peto.ramap.data.model.ReviewCommentPageResponse
import com.peto.ramap.data.model.ReviewCommentReceiptResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteReviewCommentDataSource(
    private val client: SupabaseClient,
    private val images: ReviewImageResolver,
) : ReviewCommentDataSource {
    override suspend fun fetchDetail(reviewId: String): ReviewResponse? {
        val detail =
            client.postgrest
                .rpc(
                    "fetch_review_detail",
                    buildJsonObject { put("p_review_id", reviewId) },
                ).decodeAs<ReviewResponse?>() ?: return null
        return images.resolveReviews(listOf(detail)).single()
    }

    override suspend fun fetchCounts(reviewIds: List<String>): Map<String, Int> {
        if (reviewIds.isEmpty()) return emptyMap()
        val result =
            invokeRequest {
                client.postgrest
                    .rpc(
                        "fetch_review_comment_counts",
                        buildJsonObject { put("p_review_ids", JsonArray(reviewIds.distinct().map(::JsonPrimitive))) },
                    ).decodeList<ReviewCommentCountResponse>()
                    .associate { it.reviewId to it.commentCount }
            }
        return when (result) {
            is RamapResult.Success -> result.data
            is RamapResult.Error -> emptyMap()
        }
    }

    override suspend fun fetchComments(
        reviewId: String,
        parentId: String?,
        cursor: ReviewCommentCursor?,
    ): ReviewCommentPageResponse =
        client.postgrest
            .rpc(
                "fetch_review_comments",
                buildJsonObject {
                    put("p_review_id", reviewId)
                    put("p_parent_id", parentId)
                    put("p_created_at", cursor?.createdAt)
                    put("p_comment_id", cursor?.commentId)
                },
            ).decodeAs()

    override suspend fun submitComment(submission: ReviewCommentSubmission): ReviewCommentReceiptResponse =
        client.postgrest
            .rpc(
                "create_review_comment",
                buildJsonObject {
                    put("p_review_id", submission.reviewId)
                    put("p_parent_id", submission.parentId)
                    put("p_body", submission.body)
                    put("p_request_id", submission.requestId)
                    put("p_expected_author_id", submission.expectedAuthorId)
                },
            ).decodeAs()

    override suspend fun editComment(
        commentId: String,
        body: String,
        expectedAuthorId: String,
    ): ReviewCommentReceiptResponse =
        client.postgrest
            .rpc(
                "edit_review_comment",
                buildJsonObject {
                    put("p_comment_id", commentId)
                    put("p_body", body)
                    put("p_expected_author_id", expectedAuthorId)
                },
            ).decodeAs()

    override suspend fun deleteComment(
        commentId: String,
        expectedAuthorId: String,
    ): ReviewCommentDeletionResponse =
        client.postgrest
            .rpc(
                "delete_review_comment",
                buildJsonObject {
                    put("p_comment_id", commentId)
                    put("p_expected_author_id", expectedAuthorId)
                },
            ).decodeAs()

    override suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        details: String,
        expectedAccountId: String,
    ) {
        client.postgrest.rpc(
            "report_review_comment",
            buildJsonObject {
                put("p_comment_id", commentId)
                put("p_reason", reason.name.lowercase())
                put("p_details", details)
                put("p_expected_account_id", expectedAccountId)
            },
        )
    }
}
