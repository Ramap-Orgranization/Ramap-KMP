package com.peto.ramap.image

import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.request.ImageRequest
import com.peto.ramap.domain.model.image.AuthorizedImage
import com.peto.ramap.domain.model.image.ImageAccessScope
import com.peto.ramap.domain.repository.ImageAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Buffer
import okio.FileSystem
import kotlin.random.Random
import kotlin.time.Clock

@OptIn(ExperimentalCoilApi::class)
internal class ImageCacheFixture(
    private val context: PlatformContext,
) : ImageAccessRepository,
    NetworkClient {
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ramap-cache-${Random.nextLong()}"
    val cache =
        DiskCache
            .Builder()
            .directory(directory / "images")
            .maxSizeBytes(1048576)
            .build()
    private val loaders = mutableListOf<ImageLoader>()
    override val scopes = MutableStateFlow(ImageAccessScope("viewer", 0, 0))
    var downloads = 0
    var authorizations = 0
    var allowed = true
    var protected = true
    var onDownload: () -> Unit = {}

    override fun snapshot() = scopes.value

    override fun isProtected(url: String) = protected && url.contains("/private/")

    override suspend fun authorize(url: String): AuthorizedImage? {
        authorizations++
        return if (allowed) AuthorizedImage(url, "bucket/owner/photo.jpg", snapshot()) else null
    }

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (NetworkResponse) -> T,
    ): T {
        downloads++
        onDownload()
        val now = Clock.System.now().toEpochMilliseconds()
        val policy = if (request.url.contains("/private/")) "private,no-store" else "private,max-age=86400,immutable"
        return block(
            NetworkResponse(
                requestMillis = now,
                responseMillis = now,
                headers =
                    NetworkHeaders
                        .Builder()
                        .set("Cache-Control", policy)
                        .set("Content-Type", "image/test")
                        .build(),
                body = NetworkResponseBody(Buffer().writeUtf8("image")),
            ),
        )
    }

    fun loader(
        respectHeaders: Boolean = true,
        migrate: Boolean = false,
    ): ImageLoader {
        lateinit var loader: ImageLoader
        loader =
            ImageLoader
                .Builder(context)
                .diskCache(cache)
                .components {
                    if (migrate) add(ImageCacheMigrationInterceptor { cache })
                    add(AuthorizedImageInterceptor(this@ImageCacheFixture) { loader.memoryCache })
                    add(
                        NetworkFetcher.Factory(networkClient = { this@ImageCacheFixture }, cacheStrategy = {
                            if (respectHeaders) CacheControlCacheStrategy() else CacheStrategy.DEFAULT
                        }),
                    )
                    add(
                        Decoder.Factory { result, _, _ ->
                            Decoder {
                                result.source.source().readUtf8()
                                DecodeResult(ColorImage(width = 10, height = 10, size = 400), false)
                            }
                        },
                    )
                }.build()
        loaders += loader
        return loader
    }

    suspend fun load(
        loader: ImageLoader,
        url: String,
    ) = loader.execute(
        ImageRequest
            .Builder(context)
            .data(url)
            .size(10, 10)
            .build(),
    )

    fun close() {
        loaders.forEach(ImageLoader::shutdown)
        cache.shutdown()
        FileSystem.SYSTEM.deleteRecursively(directory)
    }
}
