package com.peto.ramap.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import com.peto.ramap.data.repository.di.repositoryModule
import com.peto.ramap.domain.repository.ImageUrlRefresher
import com.peto.ramap.domain.repository.LoginRepository
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import okio.ByteString.Companion.decodeBase64
import okio.FileSystem
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.random.Random

@OptIn(SupabaseInternal::class, ExperimentalCoilApi::class)
internal class ImageLogoutCacheFixture(
    private val context: PlatformContext,
    private val decoder: Decoder.Factory?,
) {
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ramap-logout-cache-${Random.nextLong()}"
    val imageBytes = CHECK_IMAGE_BASE64.decodeBase64()!!
    var imageRequests = 0
        private set
    val authRequests = mutableListOf<String>()
    private val imageClient =
        HttpClient(
            MockEngine {
                imageRequests++
                respond(
                    content = imageBytes.toByteArray(),
                    headers = headersOf("Content-Type" to listOf("image/png"), "Cache-Control" to listOf("private,no-store")),
                )
            },
        )
    private val supabase =
        createSupabaseClient("https://auth.fixture.invalid", "fixture-key") {
            httpEngine =
                MockEngine { request ->
                    authRequests += request.url.encodedPath
                    when (request.url.encodedPath) {
                        "/auth/v1/logout" -> respond("", HttpStatusCode.NoContent)
                        "/rest/v1/rpc/unregister_push_registrations" -> respond("null", headers = headersOf("Content-Type", "application/json"))
                        else -> error("Unexpected fixture request: ${request.url.encodedPath}")
                    }
                }
            install(Auth) {
                minimalConfig()
                autoSetupPlatform = false
            }
            install(Postgrest)
        }
    private val dependencies =
        koinApplication {
            modules(
                repositoryModule,
                module { single { supabase } },
            )
        }
    val login = dependencies.koin.get<LoginRepository>()
    var cache = openCache()
        private set
    private val loaders = mutableListOf<ImageLoader>()

    suspend fun authenticate() {
        supabase.auth.importSession(
            UserSession(
                accessToken = "fixture-access-token",
                refreshToken = "fixture-refresh-token",
                expiresIn = 3600,
                tokenType = "bearer",
                user = UserInfo(id = "fixture-viewer", aud = "authenticated"),
            ),
            autoRefresh = false,
        )
    }

    fun legacyLoader(): ImageLoader =
        ImageLoader
            .Builder(context)
            .diskCache(cache)
            .components {
                add(ExpiredImageUrlInterceptor(ImageUrlRefresher { null }))
                add(KtorNetworkFetcherFactory(httpClient = imageClient))
                decoder?.let(::add)
            }.build()
            .also(loaders::add)

    suspend fun load(loader: ImageLoader) =
        loader.execute(
            ImageRequest
                .Builder(context)
                .data(IMAGE_URL)
                .size(1, 1)
                .build(),
        )

    fun reopenCache() {
        loaders.forEach(ImageLoader::shutdown)
        cache.shutdown()
        cache = openCache()
    }

    private fun openCache() =
        DiskCache
            .Builder()
            .directory(directory / "images")
            .maxSizeBytes(1048576)
            .build()

    suspend fun close() {
        loaders.forEach(ImageLoader::shutdown)
        cache.shutdown()
        imageClient.close()
        supabase.close()
        dependencies.close()
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    companion object {
        const val IMAGE_URL = "https://images.fixture.invalid/private/photo.png?token=fixture-session"
        private const val CHECK_IMAGE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGMQa9/8HwAEVwJQytRYYQAAAABJRU5ErkJggg=="
    }
}
