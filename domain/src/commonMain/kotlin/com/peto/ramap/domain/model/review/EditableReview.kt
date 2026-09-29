package com.peto.ramap.domain.model.review

data class EditableReview(
    val id: String,
    val shopId: String,
    val body: String,
    val imagePaths: List<String>,
    val imageUrls: List<String?>,
    val isPublic: Boolean,
)
