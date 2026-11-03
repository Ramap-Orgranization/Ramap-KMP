package com.peto.ramap.image

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import com.peto.ramap.domain.repository.ImageUrlRefresher
import kotlinx.coroutines.CancellationException

internal class ExpiredImageUrlInterceptor(
    private val urls: ImageUrlRefresher,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        val error = (result as? ErrorResult)?.throwable as? HttpException ?: return result
        if (error.response.code !in EXPIRY_HTTP_CODES) return result
        val originalUrl = chain.request.data as? String ?: return result
        val refreshedUrl =
            try {
                urls.refreshExpiredUrl(originalUrl)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        if (refreshedUrl == null || refreshedUrl == originalUrl) return result
        val request =
            chain.request
                .newBuilder()
                .data(refreshedUrl)
                .build()
        return chain.withRequest(request).proceed()
    }

    private companion object {
        val EXPIRY_HTTP_CODES = setOf(400, 401, 403)
    }
}
