package com.peto.ramap.data.datasource.place

import com.peto.ramap.data.model.PlaceSearchErrorResponse
import com.peto.ramap.domain.model.place.PlaceSearchErrorCode
import com.peto.ramap.domain.model.place.PlaceSearchException
import kotlinx.serialization.json.Json

internal object PlaceSearchErrorParser {
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_UNAVAILABLE = 503
    private const val RATE_LIMITED_CODE = "search_rate_limited"
    private const val MAX_RETRY_AFTER_SECONDS = 86_400L
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        status: Int,
        body: String,
        retryAfterHeader: String?,
    ): PlaceSearchException? {
        val response = runCatching { json.decodeFromString<PlaceSearchErrorResponse>(body) }.getOrNull()
        val code =
            when {
                status == HTTP_TOO_MANY_REQUESTS && response?.code == RATE_LIMITED_CODE ->
                    if (response.limitWindow == "day") PlaceSearchErrorCode.DAILY_LIMIT else PlaceSearchErrorCode.REQUEST_LIMIT
                status == HTTP_UNAVAILABLE -> PlaceSearchErrorCode.UNAVAILABLE
                else -> return null
            }
        val retryAfterSeconds =
            retryAfterHeader?.trim()?.toLongOrNull()?.takeIf { it in 1..MAX_RETRY_AFTER_SECONDS }
                ?: response?.retryAfterSeconds?.takeIf { it in 1..MAX_RETRY_AFTER_SECONDS }
        return PlaceSearchException(code, retryAfterSeconds)
    }
}
