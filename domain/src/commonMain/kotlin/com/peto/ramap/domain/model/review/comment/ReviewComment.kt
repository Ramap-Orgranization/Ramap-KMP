package com.peto.ramap.domain.model.review.comment

data class ReviewComment(
    val id: String,
    val parentId: String?,
    val body: String,
    val nickname: String,
    val createdAt: String,
    val canReport: Boolean = false,
    val canManage: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
) {
    val cursor: ReviewCommentCursor get() = ReviewCommentCursor(createdAt, id)
}
