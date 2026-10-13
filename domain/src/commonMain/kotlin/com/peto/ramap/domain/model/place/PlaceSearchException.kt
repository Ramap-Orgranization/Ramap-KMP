package com.peto.ramap.domain.model.place

class PlaceSearchException(
    val code: PlaceSearchErrorCode,
    retryAfterSeconds: Long? = null,
) : Exception(code.name) {
    val retryAfterSeconds: Long? = retryAfterSeconds?.takeIf { it in 1..MAX_RETRY_AFTER_SECONDS }

    private companion object {
        const val MAX_RETRY_AFTER_SECONDS = 86_400L
    }
}
