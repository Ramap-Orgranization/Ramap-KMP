package com.peto.ramap.platform.storage.notice

interface AppNoticeStorage {
    suspend fun fetchHiddenNoticeId(): String?

    suspend fun hideNotice(noticeId: String)
}
