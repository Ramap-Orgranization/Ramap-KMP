package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.MyReviewsPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MyReviewsPageResponse(
    val reviews: List<MyReviewResponse>,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("public_count") val publicCount: Int,
    @SerialName("private_count") val privateCount: Int,
    @SerialName("profile_is_public") val profileIsPublic: Boolean,
) {
    fun toDomain(): MyReviewsPage = MyReviewsPage(reviews.map(MyReviewResponse::toDomain), totalCount, publicCount, privateCount, profileIsPublic)
}
