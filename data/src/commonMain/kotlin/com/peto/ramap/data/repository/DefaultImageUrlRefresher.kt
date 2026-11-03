package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.image.ExpiredImageUrlParser
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.domain.repository.ImageUrlRefresher
import kotlin.time.Clock

internal class DefaultImageUrlRefresher(
    private val parser: ExpiredImageUrlParser,
    private val images: ImageUrlDataSource,
) : ImageUrlRefresher {
    override suspend fun refreshExpiredUrl(url: String): String? {
        val image = parser.parseExpired(url, Clock.System.now()) ?: return null
        return images.fetchUrls(listOf(image))[image]?.url?.takeUnless { it == url }
    }
}
