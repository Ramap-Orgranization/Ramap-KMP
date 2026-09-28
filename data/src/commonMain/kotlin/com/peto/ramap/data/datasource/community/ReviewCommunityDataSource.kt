package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.model.PublicProfileResponse

internal interface ReviewCommunityDataSource {
    suspend fun fetchBlockedUsers(): List<PublicProfileResponse>
}
