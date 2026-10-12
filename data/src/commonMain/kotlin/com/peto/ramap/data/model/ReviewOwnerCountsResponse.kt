package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewOwnerCountsResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("public_count") val publicCount: Int,
    @SerialName("private_count") val privateCount: Int,
    @SerialName("published_count") val publishedCount: Int,
    @SerialName("profile_is_public") val profileIsPublic: Boolean,
) {
    fun toDomain() = ReviewOwnerCounts(totalCount, publicCount, privateCount, publishedCount, profileIsPublic)
}
