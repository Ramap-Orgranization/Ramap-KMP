package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class NaverBookmarksResponse(
    val bookmarkList: List<NaverBookmarkResponse> = emptyList(),
)
