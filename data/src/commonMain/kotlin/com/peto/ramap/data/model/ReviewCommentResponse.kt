package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewCommentResponse(
    val id: String,
    @SerialName("parent_id") val parentId: String? = null,
    val body: String,
    val nickname: String,
    @SerialName("created_at") val createdAt: String,
    val replies: List<ReviewCommentResponse> = emptyList(),
    @SerialName("has_more_replies") val hasMoreReplies: Boolean = false,
    @SerialName("can_report") val canReport: Boolean = false,
    @SerialName("can_manage") val canManage: Boolean = false,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
) {
    fun toComment(): ReviewComment =
        ReviewComment(
            id = id,
            parentId = parentId,
            body = body,
            nickname = nickname,
            createdAt = createdAt,
            canReport = canReport,
            canManage = canManage,
            isEdited = isEdited,
            isDeleted = isDeleted,
        )

    fun toThread(): ReviewCommentThread =
        ReviewCommentThread(
            comment = toComment(),
            replies = replies.map(ReviewCommentResponse::toComment),
            nextReplyCursor =
                replies
                    .lastOrNull()
                    ?.toComment()
                    ?.cursor
                    ?.takeIf { hasMoreReplies },
        )
}
