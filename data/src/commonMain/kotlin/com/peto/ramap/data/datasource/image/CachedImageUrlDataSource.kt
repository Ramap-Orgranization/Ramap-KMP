package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

internal class CachedImageUrlDataSource(
    private val remote: ImageUrlDataSource,
    private val accessPolicy: ImageUrlAccessPolicy,
) : ImageUrlDataSource {
    private val mutex = Mutex()
    private val urls = mutableMapOf<SignedImageRequest, SignedImageUrl>()
    private var cachedAccess: ImageUrlAccessSnapshot? = null

    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
        if (images.isEmpty()) return emptyMap()
        return mutex.withLock {
            val baseline = accessPolicy.snapshot()
            cachedAccess?.let { access -> accessPolicy.invalidatedImages(access, urls.keys).forEach(urls::remove) }
            cachedAccess = baseline
            val unique = images.distinct()
            val expiresAfter = Clock.System.now() + REFRESH_MARGIN
            val missing = unique.filter { urls[it]?.expiresAt?.let { expiry -> expiry > expiresAfter } != true }
            if (missing.isNotEmpty()) {
                val fetched = remote.fetchUrls(missing)
                check(accessPolicy.invalidatedImages(baseline, unique).isEmpty()) { ERROR_ACCESS_CHANGED }
                for ((key, value) in fetched) {
                    if (value != null) urls[key] = value else urls.remove(key)
                }
            }
            check(accessPolicy.invalidatedImages(baseline, unique).isEmpty()) { ERROR_ACCESS_CHANGED }
            val result = unique.associateWith { urls[it] }
            while (urls.size > CACHE_LIMIT) urls.remove(urls.keys.first())
            result
        }
    }

    private companion object {
        const val CACHE_LIMIT = 512
        const val ERROR_ACCESS_CHANGED = "Image access changed"
        val REFRESH_MARGIN = 1.minutes
    }
}
