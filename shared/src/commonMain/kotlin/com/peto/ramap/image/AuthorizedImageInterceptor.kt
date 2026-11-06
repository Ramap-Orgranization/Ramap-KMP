package com.peto.ramap.image

import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.peto.ramap.domain.repository.ImageAccessRepository
import kotlinx.coroutines.CancellationException

internal class AuthorizedImageInterceptor(
    private val access: ImageAccessRepository,
    private val memoryCache: () -> MemoryCache?,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val url = chain.request.data as? String ?: return chain.proceed()
        if (!access.isProtected(url)) return chain.proceed()
        try {
            val image = access.authorize(url) ?: return denied(chain)
            if (access.snapshot() != image.scope) return denied(chain)
            val request =
                chain.request
                    .newBuilder()
                    .data(image.url)
                    .memoryCacheKey(image.memoryKey)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build()
            val result = chain.withRequest(request).proceed()
            if (access.snapshot() == image.scope) return result
            if (result is SuccessResult) result.memoryCacheKey?.let { memoryCache()?.remove(it) }
            return denied(chain)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return denied(chain)
        }
    }

    private fun denied(chain: Interceptor.Chain): ErrorResult = ErrorResult(null, chain.request, IllegalStateException(ERROR_ACCESS_DENIED))

    private companion object {
        const val ERROR_ACCESS_DENIED = "Image access unavailable"
    }
}
