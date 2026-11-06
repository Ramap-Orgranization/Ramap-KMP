package com.peto.ramap.domain.repository

fun interface ImageUrlRefresher {
    suspend fun refreshExpiredUrl(url: String): String?
}
