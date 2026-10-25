package com.peto.ramap.domain.model.community

data class CommunityFeedPage(
    val reviews: List<ProfileReview> = emptyList(),
    val nextCursor: CommunityFeedCursor? = null,
)
