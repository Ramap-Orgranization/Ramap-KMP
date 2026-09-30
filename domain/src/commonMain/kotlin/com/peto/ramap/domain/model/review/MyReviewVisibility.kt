package com.peto.ramap.domain.model.review

enum class MyReviewVisibility(
    val rpcValue: String,
) {
    ALL("all"),
    PUBLIC("public"),
    PRIVATE("private"),
}
