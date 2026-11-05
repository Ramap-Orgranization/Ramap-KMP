package com.peto.ramap.domain.model.image

data class ImageAccessScope(
    val userId: String?,
    val sessionEpoch: Long,
    val imageRevision: Long,
) {
    fun memoryKey(reference: String): String = "$MEMORY_KEY_PREFIX:$userId:$sessionEpoch:$imageRevision:$reference"

    private companion object {
        const val MEMORY_KEY_PREFIX = "private"
    }
}
