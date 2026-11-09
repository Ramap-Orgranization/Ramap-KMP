package com.peto.ramap.data.datasource.announcement

internal interface AnnouncementReadDataSource {
    suspend fun fetchReadIds(accountId: String): Set<String>

    suspend fun markRead(
        announcementId: String,
        accountId: String,
    )
}
