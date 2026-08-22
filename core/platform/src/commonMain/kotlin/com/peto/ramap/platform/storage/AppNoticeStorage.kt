package com.peto.ramap.platform.storage

interface AppNoticeStorage {
    suspend fun fetchHiddenNoticeId(): String?

    suspend fun hideNotice(noticeId: String)
}
