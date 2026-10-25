package com.peto.ramap.data.datasource.image

internal data class ImageUrlAccessSnapshot(
    val userId: String?,
    val epoch: Long,
    val revision: Long,
)
